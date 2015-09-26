(ns clj-cloud-gen.cloudformation
  ^{:author "Matthew Burns"
    :description "Transform the results of invoking a function graph describing the infrastructure into AWS Cloudformation Template."}
  (:require [cheshire.core :as json :refer [generate-string]]
            [clojure.algo.generic.functor :refer [fmap]]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [plumbing.core :as p :refer [fnk]]
            [plumbing.graph :as graph]))

(def type->aws-resource {:vpc "VPC"
                         :subnet "Subnet"
                         :route "Route"
                         :route-table "RouteTable"
                         :node "Instance"
                         :internet-gateway "InternetGateway"
                         :gateway "InternetGateway"
                         :internet "VPCGatewayAttachment"
                         :subnet-route-table "SubnetRouteTableAssociation"
                         :security-group "SecurityGroup"
                         :volume "Volume"
                         :ip "EIP"
                         :instance "Instance"
                         :volume-attachment "VolumeAttachment"})

(defn camel->keyword
  [s]
  (->> (str/split s #"(?<=[a-z])(?=[A-Z])") (map str/lower-case) (interpose \-) str/join keyword))

(defn keyword->camel
  [kw]
  (->> (str/split (.replace (name kw) "?" "") #"\-") (fmap str/capitalize) str/join))

(defn key->string
  "Given a map converts all keys that are keywords from dash-separated to camel-case using key->string."
  [m]
  (let [f (fn [[k v]] [(keyword->camel k) v])
        walk-fn (fn [x] (if (map? x) (->> (map f x) (into {})) x))]
    (walk/postwalk walk-fn m)))

(defn key->key-id
  [k]
  (keyword (format "%s-id" (name k))))

(defn aws-fn
  [fn-name & args]
  {(format "Fn::%s" (keyword->camel fn-name))
   (if (= 1 (count args))
     (first args)
     args)})

(defn resource-references?
  [resource type]
  (if (contains? resource (key->key-id type)) true false))

(defn resource-referenced
  [resource]
  (filter #(resource-references? resource %) (keys type->aws-resource)))

(defn resource-referenced?
  [resource]
  (if (resource-referenced resource) true false))

(defn transform-reference
  "Camel cases and fixes up names."
  [resource-data resource-type]
  (let [resource-name (get resource-data (key->key-id resource-type))]
    {"Ref" (keyword->camel resource-name)}))

(defn transform-properties-references
  [properties]
  (if (resource-referenced? properties)
    (reduce (fn [xs x]
              (update-in xs [(key->key-id x)] (fn [n] (transform-reference properties x))))
            properties
            (resource-referenced properties))
        properties))

(defn resource
  "Return a resource item. Sections other than resource properties like metadata specified via named optional arguments."
  [name type properties & kv]
  (let [props (apply hash-map :properties (transform-properties-references (dissoc properties :depends-on)) :type type kv)
        deps (if-let [d (:depends-on properties)] d nil)
        args (if-let [a deps] (assoc props :depends-on (keyword->camel deps)) props)]
    (vector (keyword->camel name) args)))

(defn- render-dispatch [{:keys [type] :as args}] type)

(defmulti render-resource #'render-dispatch)

(defmethod render-resource :default
  [{:keys [type properties name] :as args}]
  (let [r (resource name (format "AWS::EC2::%s" (get type->aws-resource type))  properties)]
        [(first r) (key->string (last r))]))

(defmethod render-resource :node
  [{:keys [type properties name] :as args}]
  (let [new-sg (map (fn [group-id] {"Ref" (keyword->camel group-id)})    (:security-group-ids properties))
        node-resource (resource name (format "AWS::EC2::%s" (get type->aws-resource type))  (assoc-in properties [:security-group-ids] new-sg))]
    [(first node-resource) (key->string (last node-resource))]
     ))

(defmethod render-resource :volume
  [{:keys [type properties name] :as args}]
  (let [resource-name (keyword->camel name)
        props (dissoc properties :depends-on)
        deps (if-let [d (:depends-on properties)] (keyword->camel d) nil)
        resource-type (format "AWS::EC2::%s" (get type->aws-resource type))
        s0 {"Type" resource-type
            "Properties" (key->string props)}
        data (if-let [s deps]
               (assoc-in s0 ["Properties" "AvailabilityZone"] {"Fn::GetAtt" [deps "AvailabilityZone"]})
               s0)]
    (vector resource-name data)))

(defmethod render-resource :gateway
  [{:keys [type properties name] :as args}]
  (let [r (resource name (format "AWS::EC2::%s" (get type->aws-resource type)) properties)]
    [(first r) (key->string (last r))]))

(defn transform-resource
  [type name resource]
   (render-resource {:name name :type type :properties resource}))

(defn transform-type-resources
  [resources type]
  (map (fn [r] (transform-resource  type (first r) (last r))) (seq resources)))

(defn transform-resources
  [resources]
  (map (fn [r] (transform-type-resources (last r) (first r))) (seq resources)))

(defn resource-item-list
  [resource]
  (zipmap (map #(-> % first) resource)  (map #(-> % last) resource)))

(defn resources->map
  "Given a list of resources or outputs with the form [name map] returns a map
  where name is the key and map is the value."
  [resources]
  (reduce (fn [xs x] (merge xs x)) {} (map resource-item-list resources)))

(defn parts->template
  "Returns a json cloudformation template."
  [format-version resources outputs]
  (json/generate-string
   {"Resources" (resources->map (transform-resources resources))
    "AWSTemplateFormatVersion"  (or format-version "2010-09-09")
    } {:pretty true}))

(defn assemble-cloudformation
  "Creates and invokes a function graph that describes the various resources
   and their relations for creating the IT infrastructure."
  [specification it-layout settings]
  ((graph/eager-compile specification) (merge it-layout settings)))


(defn ->tags [keys div tags]
  "Turns a list of tag definitions into an aws tag list.
   Tags is a list with the repeating format \"key value\" or
   \"key value propagate-at-launch?\". The format must be consistant
   for the entire list.
   Div is the number of elements in each tag definition (either 2 or 3).
   Keys is a list of keys that will become the keys for the parts of a
   tag definition."
  (->> (partition div tags)
       (map #(zipmap keys %))))

(defn tags
  "Helper to create aws tags. It should accept the repeating format
  \"key value\" or \"key value propagate-at-launch?\" for scaling
  group tags.
  Ex: non-scaling goup (tags :stack-name \"my-stack\"
  :purpose \"database\")
  Ex: scaling group (tags :stack-name \"my-stack\" true
  :purpose \"database\" false)"
  [& kvs]
  (let [arg-count (count kvs)]
    (if (and
         (-> (mod arg-count 3) (= 0))
         (->> (nthnext kvs 2)
              (take-nth 3)
              (every? #(or (true? %) (false? %)))))
      (->tags [:key :value :propagate-at-launch] 3 kvs)
      (do (-> (mod arg-count 2)
              (= 0)
              (assert))
          (->tags [:key :value] 2 kvs)))))

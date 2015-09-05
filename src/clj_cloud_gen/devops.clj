(ns clj-cloud-gen.devops
  (:require [aws.sdk.s3 :as s3]
            [clojure.java.io :as io]
            [com.palletops.awaze.cloudformation :as cf :refer [create-stack create-stack-map list-stacks-map describe-stacks]]
            [environ.core :refer [env]]
            [plumbing.core :as p :refer [fnk]]
            [plumbing.graph :as graph]))

(def setup-graph
  {
;;   :existing-instances (p/fnk [credentials] (ec2/describe-instances credentials))
   :existing-stacks (p/fnk [credentials] (cf/list-stacks-map credentials))
   :cf-json-template (p/fnk [template-path] (slurp (io/resource template-path)))
   :create-stack-map (p/fnk [credentials cf-stack-args] (cf/create-stack-map credentials cf-stack-args))
;;   :stack-resources (p/fnk [credentials cf-stack-args] (cf/describe-stack-resources credentials cf-stack-args))
   :update-stack-map (p/fnk [cf-stack-args credentials] (cf/update-stack-map credentials cf-stack-args))
   :delete-stack-map (p/fnk [cf-stack-args credentials] (cf/delete-stack-map credentials cf-stack-args))})

(def setup (graph/eager-compile setup-graph))
(def creds {:access-key (env :aws-access-key) :secret-key (env :aws-secret-key)})


(defn delete-stack-by-pieces [credentials stack-name]

  )

(defn stack-exists?
  "Check to see if the stack already is present and created."
  [credentials stack-name]
  (let [stacks (-> credentials describe-stacks)]
    (if (some #{stack-name} (map (fn [stack] (:stack-name stack)) (-> stacks :stacks))) true false)))

(defn create-vpc-stack
  "Build up a virtual private cloud."
  [interface]
  (cf/cloudformation (:create-stack-map interface)))

(defn delete-vpc-stack
  "Update a current existing virtual private cloud."
  [{:keys [credentials stack-name] :as args}]
  (if (stack-exists? credentials stack-name)
    (cf/delete-stack credentials {:name stack-name}) nil))

(defn list-stacks
  [interface]
  (cf/cloudformation (:existing-stacks interface)))

(defn update-template
  "Put the output template json into a bucket that the cloudformation will use
   to create a stack."
  [{:keys [credentials bucket object-name interface]}]
  (s3/put-object credentials bucket object-name (:cf-json-template interface)))

(defn update-vpc-stack
  "Update a current existing virtual private cloud."
  [{:keys [template-path credentials stack-name template-url bucket object-name] :as args}]
  (let [settings {:cf-stack-args {:template-url template-url :stack-name stack-name} :credentials credentials :template-path template-path}
        interface (setup settings)
        cfgs {:credentials credentials :bucket bucket :object-name object-name :template (:cf-json-template interface)}
        resp (update-template cfgs)]
  (cf/cloudformation (:update-stack-map interface))))

(defn create-update-cloud
  "Update the cloud if it exists and create the stack if needed."
  [{:keys [template-path credentials stack-name template-url bucket object-name] :as args}]
  (let [settings {:cf-stack-args {:template-url template-url :stack-name stack-name} :credentials credentials :template-path template-path}
        interface (setup settings)
        cfgs {:credentials credentials :bucket bucket :object-name object-name :template (:cf-json-template interface)}
        resp (update-template cfgs)
        result (if (stack-exists? interface stack-name) (update-vpc-stack interface) (create-vpc-stack interface))]
    {
;;     :stack-results result
;;     :s3-results resp
     :interface interface
     }))

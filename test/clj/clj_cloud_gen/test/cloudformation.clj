(ns clj-cloud-gen.test.cloudformation
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [plumbing.core :as p :refer [fnk]]
            [plumbing.graph :as graph]
            [clojure.walk :refer :all]
            [clj-cloud-gen.cloud-specification :refer :all]
            [clj-cloud-gen.cloudformation :refer :all]))

(def test-environment-layout
  {:test-cfgs
   (p/fnk []
          {:description "DEVOPS VPC"
           :format  "2010-09-09"
           :parameters {:vpc-id "devops-vpc"
                        :key-name "devops_prd"
                        :ssh-from "0.0.0.0/0"
                        :instance-type "t2.micro"}
           :mappings {:availability-zone "us-east-1"
                      :instance-arch  {:c3.large {:arch "HVM64"}}
                      :region  {:us-east-1 {:pv64 "ami-50842d38"
                                            :hvm64 "ami-08842d60"
                                            :hvmg2 "ami-3a329952"}}
                      :image {:web-node {:ami "ami-116d857a"
                                         :volume {:size "100" :mount-point "/dev/xvdf"}}
                              :nat {:ami "ami-b0210ed8"}}
                      :subnet-config {:vpc {:cidr "10.0.0.0/16"}
                                      :public {:cidr "10.0.0.0/24"
                                               :nat {:ip {:private  "10.0.0.40"}}}
                                      :private {:cidr "10.0.1.0/24"
                                                :default {:ip {:private "10.0.1.30"}}}}}})
   :edn-template
   (p/fnk [test-cfgs] (assemble-cloudformation parameteric-cfgs static-cfgs test-cfgs))

   :test-resources-map
   (p/fnk [edn-template] (-> edn-template :resources))
  }
)

(def test-environment (graph/eager-compile test-environment-layout))
(def te (test-environment {}))
(def tr (-> te :test-resources-map))

(deftest test-key->string
  (is (= "OneFoo" (keyword->camel :one-foo)))
  (is (= {"OneFoo" 1, "TwoBar" 2} (key->string {:one-foo 1 :two-bar 2}))))

(deftest test->tags
  (is (= (:->tags te) [{"Key" "Name" "Value" "blah"} {"Key" "Network" "Value" "foo"}])))

(deftest test-tags
  (is (= (tags :stack-name "my-stack" :purpose "database")
         (list {:key :stack-name :value "my-stack"} {:key :purpose, :value "database"}))))

(deftest test-resource-reference
  (is (resource-references? {:vpc-id "devops" :image-id "ami-e565ba8c" :instance-type "m1.large"}  :vpc))
  (is (resource-referenced? {:vpc-id "devops" :image-id "ami-e565ba8c" :instance-type "m1.large"}))
  (is (resource-references? {:vpc-id "devops" :image-id "ami-e565ba8c" :instance-type "m1.large"}  :subnet))
  (is (resource-referenced? {:vpce-id "devops" :image-id "ami-e565ba8c" :instance-type "m1.large"}))
  (def p (-> tr :route :public))
    (is (resource-referenced p))
  (is (= (transform-reference :route p) {"VpcId" {"Ref" "DEVOPS"}, "CidrBlock" "10.0.1.0/24"}))
  (is (= (transform-reference :vpc p) {"VpcId" {"Ref" "DEVOPS"}, "CidrBlock" "10.0.1.0/24"}))
  (is (= (transform-properties-references p) (list {"VpcId" {"Ref" "DEVOPS"}, "CidrBlock" "10.0.1.0/24"})))
  (is (= (transform-properties-references p) {:depends-on :default-gateway, :route-table-id {"Ref" :public}, :destination-cidr-block "0.0.0.0/0", :gateway-id {"Ref" :default}})))

(deftest test-resource
  (map resource-item-list (list (resource :public :route (-> tr :route :public))))

  (is (= (resource :public :route (-> tr :route :public))
         (resource :devops-public :subnet (-> tr :subnet :devops-public))
         (resource :devops :vpc (-> tr :vpc :devops))
         ()
         ["WebLC" {:properties {:image-id "ami-e565ba8c", :instance-type "m1.large"}, :type "AWS::AutoScaling::LaunchConfiguration", :metadata {"AWS::CloudFormation::Init" {"config" {:foo 1}}}}])))

(deftest test-render-resource
  ;;  (def ts (-> tr :subnet :devops-public))
  (def ts (-> tr :route :public))
  (is (= (render-resource {:type :subnet :properties ts :name :devops-public})
         ["DevopsPublic" {:properties {:depends-on :default-gateway, :route-table-id {"Ref" :public}, :destination-cidr-block "0.0.0.0/0", :gateway-id {"Ref" :default}}, :type "AWS::EC2::Subnet"}])))

(deftest test-transform-resource
  (is (= (transform-resource :route :public  (-> tr :route :public))
         ["Public" {:properties {:depends-on :default-gateway, :route-table-id {"Ref" :public}, :destination-cidr-block "0.0.0.0/0", :gateway-id {"Ref" :default}}, :type "AWS::EC2::Route"}]))
  (is (=  (count (transform-resources tr)) 11)))

(deftest test-resources->map
  (pprint (transform-resources tr))
  (resource-item-list (first (transform-resources tr)))
  (resources->map (transform-resources tr)))

(deftest test-parts->template
  "Resoure list has lists it' items.
   Each item contains a list of resources of a certain resource type.
   Each resource item is made up of a vector pair
   [:resource-name {:properties :map}]"
  (def trs (transform-resources tr))
  (def got (parts->template "2010-09-09" tr ["foo.json"]))
  (def wanted (slurp "resources/cloudformation/devops_prd.json"))
  (-> got (= wanted) (is true))
  (spit "./test_result.json" got)

  (def tfn (aws-fn :get-att "Foo"))
  (key->string tfn)
  (println trs)
  (println got)
  (pprint (resources->map trs)))

(comment
  (def got (parts->template "2010-09-09" tr ["foo.json"]))
  (spit (format "%s/%s" (System/getProperty "user.dir") "./resources/cloudformation/sample_devops_infrastructure.json") got)
)

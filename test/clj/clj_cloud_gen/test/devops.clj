(ns clj-cloud-gen.test.devops
    (:require [clojure.java.io :as io]
            [clj-cloud-gen.devops :as devops :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [plumbing.core :as p :refer [fnk]]
            [plumbing.graph :as graph]
            [environ.core :refer [env]]))

(def tmpl-url (:cf-template-bucket-uri env))
(def sn (:cf-stack-name env))
(def test-args {:template-path (:cf-template-path env)
                :credentials creds
                :bucket (:cf-bucket env)
                :cf-stack-args {:template-url tmpl-url :stack-name sn}
                :template-url tmpl-url
                :object-name (:cf-object-name env)
                :stack-name (:cf-stack-name env)})

(deftest test-update-template
  (def interface (devops/cloud-setup test-args))
  (devops/update-template (assoc test-args :interface interface)))

(deftest test-stack-exists?
  (is (= true (stack-exists? devops/creds sn))))

(deftest test-stack-delete
  (devops/delete-vpc-stack {:credentials devops/creds :name sn}))

(deftest test-cloud-creation
  (def interface (devops/cloud-setup test-args))
  (def create-vpc-result (devops/create-vpc-stack interface))
  (def create-result  (devops/create-update-cloud test-args)))

(deftest test-update-vpc-stack
  (def interface (devops/cloud-setup test-args))
  (update-vpc-stack test-args))

(comment
(def interface (devops/cloud-setup test-args))
(def create-vpc-result (devops/create-vpc-stack interface))
;;(update-vpc-stack test-args)
)

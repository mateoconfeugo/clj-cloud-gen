(ns clj-cloud-gen.test.devops
    (:require [clojure.java.io :as io]
            [clj-cloud-gen.devops :as devops :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [plumbing.core :as p :refer [fnk]]
            [plumbing.graph :as graph])
)

(def tmpl-url "https://s3.amazonaws.com/cf-templates-1vun595xdt6c1-us-east-1/test-ci-devops")
(def sn "devops-ci")
(def test-args {:template-path "cloudformation/test_result.json"
                :credentials creds
                :bucket "cf-templates-1vun595xdt6c1-us-east-1"
                :cf-stack-args {:template-url tmpl-url :stack-name sn}
                :template-url tmpl-url
                :object-name "test-ci-devops"
                :stack-name "devops-ci"})

(deftest test-update-template
  (def interface (devops/setup test-args))
  (devops/update-template (assoc test-args :interface interface))
  )

(deftest test-stack-exists?
  (is (= true (stack-exists? devops/creds "devops-ci"))))

(deftest test-stack-delete
  (devops/delete-vpc-stack {:credentials devops/creds :name "devops-ci"}))

(deftest test-cloud-creation
  (def interface (devops/setup test-args))
  (def create-vpc-result (devops/create-vpc-stack interface))
  (def create-result  (devops/create-update-cloud test-args)))

(deftest test-update-vpc-stack
  (def test-args {:template-path   "cloudformation/test_result.json"
                :credentials creds
                :bucket "cf-templates-1vun595xdt6c1-us-east-1"
                :cf-stack-args {:template-url tmpl-url :stack-name sn}
                :template-url tmpl-url
                :object-name "test-ci-devops"
                  :stack-name "devops-ci"})
  (def interface (devops/setup test-args))
  (update-vpc-stack test-args)  )

(def test-args {:template-path   "cloudformation/test_result.json"
                :credentials creds
                :bucket "cf-templates-1vun595xdt6c1-us-east-1"
                :cf-stack-args {:template-url tmpl-url :stack-name sn}
                :template-url tmpl-url
                :object-name "test-ci-devops"
                :stack-name "devops-ci"})

(comment
(def interface (devops/setup test-args))
(def create-vpc-result (devops/create-vpc-stack interface))
;;(update-vpc-stack test-args)
)

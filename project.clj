(defproject clj-cloud-gen "0.1.1"
  :description "AWS Cloudformation declarations of network infrastructure in clojure data structures and function graphs rather than in AWS JSON Cloudformation template language.  Also a command line program to create and update the cloud."
  :url "http://mateoconfeugo.github.io/clj-cloud-gen"
  :license {:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
;;  :main clj-cloud-gen.core.main
;;  :aot :all
  :dependencies [[org.clojure/clojure "1.7.0-RC1"] ;; LISP on the JVM
                 [clj-aws-ec2 "0.5.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [com.palletops/awaze "0.1.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-aws-s3 "0.3.10"]
                 [amazonica "0.3.30"]
                 [clj-time "0.9.0"] ; joda data time library
                 [cheshire "5.4.0" :exclusions [com.fasterxml.jackson.core/jackson-databind
                                                com.fasterxml.jackson.core/jackson-core
                                                com.fasterxml.jackson.core/jackson-annotations]]
                 [com.taoensso/timbre "3.4.0"] ; logging
                 [org.clojure/core.async "0.1.346.0-17112a-alpha" :exclusions [org.clojure/core.cache]] ;; CSP go like routines and channels
                 [environ "1.0.0"] ;; Environment configuration
                 ;;                 [org.clojure/core.match "0.3.0-alpha4" :exclusions [org.clojure/core.cache]] ;; An optimized pattern matching library
                 [org.clojure/core.match "0.3.0-alpha4"] ;; An optimized pattern matching library
                 [prismatic/plumbing "0.3.3"] ;; function graphs
                 [org.clojure/clojure "1.6.0"]]
    :profiles  {:dev {:plugins [[com.palletops/pallet-lein "0.8.0-alpha.1"]]}
                :leiningen/reply {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.12"]]
                                  :exclusions [commons-logging]}})

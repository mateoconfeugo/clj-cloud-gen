(ns clj-cloud-gen.core
  (require [clojure.tools.cli :refer [cli parse-opts]]
           [clj-cloud-gen.devops]
           [clj-cloud-gen.cloudformation]))

(def app-arguments [["-h" "--help" "Print this help" :default false :flag true]
                    ["-d" "--dry-run" "Transform but don't upload template to S3 and create/update stack." :default false :flag true]
                    ["-b" "--bucket-uri" "S3 bucket to upload the transformed cloudformation to be used during stack creation/update."]
                    ["-o" "--output" "Output the cloudformation template json data." :default nil]
                    ["-n" "--name"  "Name of the stack"]
                    ["-a" "--access-key" "AWS Access Key" :default nil]
                    ["-s" "--secret-key" "AWS Secret Key" :default nil]])

(defn main [& args]
  (let [[ opts args banner] (parse-opts args app-arguments)]
    (when (:help opts) (println banner))))

(ns clj-cloud-gen.test.core
  (refer-clojure :exclude [update])
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [clj-cloud-gen.core :as devops :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [environ.core :refer [env]]
            [plumbing.core :as p :refer [fnk]]
            [plumbing.graph :as graph]))

(let [[opts args banner] (parse-opts ["-h" "-s sdkfjsfk" "-a jskdfjsdkjf"] app-arguments)]
  (pprint opts))

(def args-vector [["-p" "--port" "PORT" :default 6088
                   :parse-fn #(Integer/parseInt %)
                   :validate [#(< 0 % 0x10000) "Must be a number between 0 and 655536"]]])

(parse-opts ["-p=12"] args-vector)

(apply parse-opts ["-p" "12"] args-vector)
(parse-opts ["--port=12"] args-vector)


(main ["-h" "-s sdkfjsfk" "-a jskdfjsdkjf"])

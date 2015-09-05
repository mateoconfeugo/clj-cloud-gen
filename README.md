# clj-cloud-gen

This is beta software - I will be changing things

A clojure library and application to create and deploy Amazon Web Services cloudformation templates.

## Usage
```clojure

(def options
          {:description "LTD VPC"
           :format  "2010-09-09"
           :parameters {:vpc-id "ltd-vpc"
                        :key-name "ltd_prd"
                        :ssh-from "0.0.0.0/0"
                        :instance-type "t2.micro"}
           :mappings {:instance-arch  {:c3.large {:arch "HVM64"}}
                      :region  {:us-east-1 {:pv64 "ami-50842d38"
                                            :hvm64 "ami-08842d60"
                                            :hvmg2 "ami-3a329952"}}
                      :image-id {:default "ami-116d857a"
                                 :nat "ami-303b1458"}
                      :subnet-config {:vpc {:cidr "10.0.0.0/16"}
                                      :public {:cidr "10.0.0.0/24"
                                               :nat {:ip {:private  "10.0.0.2"}}}
                                      :private {:cidr "10.0.1.0/24"
                                                :default {:ip {:private "10.0.1.3"}}}}}})
                                                
(def json-template (assemble-cloudformation parameteric-cfgs static-cfgs test-cfgs))                                                
```

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

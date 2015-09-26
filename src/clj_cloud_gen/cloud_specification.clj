(ns clj-cloud-gen.cloud-specification
  (:require [plumbing.core :as p :refer [fnk]]
            [plumbing.graph :as graph]))

(def static-cfgs
  {
   :gateway {:default-gateway {}}

   :internet {:default-connection {:vpc-id :devops
                                   :internet-gateway-id :default-gateway}}

   :route-table {:public-table {:vpc-id :devops}
                 :private-table {:vpc-id :devops}}

   :route {:public-route {:depends-on :default-connection
                          :route-table-id :public-table
                          :destination-cidr-block "0.0.0.0/0"
                          :gateway-id :default-gateway}
           :private-route {:route-table-id :private-table
                           :destination-cidr-block "0.0.0.0/0"
                           :instance-id :nat-node}}

   :subnet-route-table {:private-subnet-route {:subnet-id :private-subnet
                                               :route-table-id :private-table}
                        :public-subnet-route  {:subnet-id :public-subnet
                                               :route-table-id :public-table}}

   :ip {:nat-ip-address {:depends-on :default-connection
                         :domain "vpc"
                         :instance-id :nat-node}}
   })

(def parameteric-cfgs
  {:format
   (p/fnk [])

   :vpc
   (p/fnk [mappings]
          {:devops {:cidr-block (-> mappings :subnet-config :vpc :cidr)}})

   :subnet
   (p/fnk [parameters mappings]
          {:public-subnet {:vpc-id :devops :cidr-block (-> mappings :subnet-config :public :cidr)}
           :private-subnet {:vpc-id :devops :cidr-block (-> mappings :subnet-config :private :cidr)}})

   :security-group
   (p/fnk [parameters]
          (let [{:keys [ssh-from]} parameters]
            {:web {:vpc-id :devops :group-description "Enable  SSH access from anywhere"
                   :security-group-ingress [{:ip-protocol "tcp" :from-port "22" :to-port "22" :cidr-ip ssh-from}
                                            {:ip-protocol "tcp" :from-port "80" :to-port "80" :cidr-ip "0.0.0.0/0"}
                                            {:ip-protocol "tcp" :from-port "443":to-port "443" :cidr-ip "0.0.0.0/0"}]}
             :nat  {:vpc-id :devops :group-description "NAT "
                    :security-group-ingress [{:ip-protocol "icmp" :from-port "-1" :to-port "-1" :cidr-ip "0.0.0.0/0"}
                                             {:ip-protocol "tcp" :from-port "22" :to-port "22" :cidr-ip ssh-from}
                                             {:ip-protocol "tcp" :from-port "80" :to-port "80" :cidr-ip "0.0.0.0/0"}
                                             {:ip-protocol "tcp" :from-port "443":to-port "443" :cidr-ip "0.0.0.0/0"}]}
             :database {:vpc-id :devops :group-description "Pass all traffic"
                        :security-group-ingress [{:ip-protocol "tcp" :from-port "3306" :to-port "3306" :cidr-ip "0.0.0.0/0"}]}
             }))

   :node
   (p/fnk [parameters subnet mappings security-group]
          {:web-node  {:key-name (-> parameters :key-name)
                       :instance-type "c3.large"
                       :subnet-id :private-subnet
                       :private-ip-address (-> mappings :subnet-config :private :default :ip :private)
                       :source-dest-check "false"
                       :image-id  (-> mappings :image :web-node :ami)
                       :security-group-ids [:web]}
           :nat-node {:key-name (-> parameters :key-name)
                      :instance-type (-> parameters :instance-type)
                      :subnet-id :public-subnet
                      :private-ip-address (-> mappings :subnet-config :public :nat :ip :private)
                      :source-dest-check "false"
                      :security-group-ids [:nat]
                      :image-id  (-> mappings :image :nat-node :ami)}})

   :volume
   (p/fnk [parameters mappings]
          {:default-volume {:size  (-> mappings :image-id :web-node :volume :size)
                            :depends-on :web-node
                            :availability-zone (-> mappings :availability-zone)}})

   :volume-attachment
   (p/fnk [mappings]
          {:instance-mount-point {:instance-id :web-node
                                  :volume-id :default-volume
                                  :device (-> mappings :image :web-node :volume :mount-point)}})
   :resources
   (p/fnk [vpc subnet internet gateway route-table route security-group node ip volume volume-attachment subnet-route-table :as args]
          args
          )
   }
  )

---
layout: post
title:  "Apache James latest Docker images changes"
date:   2019-08-30  09:09:47 +0100
categories: james update
---

We have decided to change the latest Docker images behaviour.

Since nowdays, such images were built on the `master` branch for each products (`linagora/james-memory`, `linagora/james-cassandra-rabbitmq-ldap-project`, ...).  
This is not the way `latest` docker image should be, this [blog post](https://blog.container-solutions.com/docker-latest-confusion) is explaining this kind of misunderstood.  

So we decided to follow the global Docker users behaviour which is to clone the _latest stable release_ as the Docker latest images.

NB: to avoid such confusion, we strongly encourage you to not use the `latest` images for any of your production deployment.

Thanks for reading,
Antoine
Eventer
=======

This application defines a RESTful API for an eventing service using Play Framework 2.0, the play-salat module
and MongoDB that will run on both localhost as well as on Heroku. 

There is currently no concept of user creation, nor authentication, but the RESTful APIs are all laid out
with examples of what you will need to read, write to MongoDB. 

Please see conf/routes for the definition of these APIs. 

Play Framework 2.0, play-salat, and MongoDB will need to be installed as pre-requisites. 

To deploy on Heroku, you will need to add a MongoDB project (MongoHQ free was used for this) and the 
conf/prod-application.conf mongodb uri needs to be updated.


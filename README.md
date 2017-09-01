# Apache Camel Sample Application using REST DSL and Spring Security

This project contains an advanced setup of an Apache Camel driven application which used the REST DSL feature to provide some services exposed via Jetty. It also uses Spring security to restrict access to certain routes. 

An invoked Camel route exposed via REST DSL will extract user credentials from a received `Authorization` HTTP header and retrieve the user information from a MongoDB based authentication provider and user details service if the route invokes the `SpringSecurityContextLoader` bean and defines one of the two currently available authorization policies defined in the `SpringSecurityConfig`. Currently only the `SampleFileRoute` does make use of this feature as it will upload files to an S3 bucket and download the file again on retrieving details on an uploaded file.

Within this application, users contain two different approaches on how to store sensitive data via MongoDB/Morphia. Passwords are hashed in a way that they are very unlikely to get converted back to their original form and `PasswordAuthenticationProvider` will actually perform a check of a provided password, after hashing it, with the stored password hash. User keys on the other hand are hashed also but stored with a salt value in order to convert the key later on back to its original form. `UserKeyAuthenticationProvider` therefore will check the provided user key with the one stored in the database (the unhashed value of it).

The application presented here is primarily intended to play around with certain features and see how these play in together.

Note: The actual application configuration properties are excluded as they contain real AWS credentials!

## API Usage

### Check for online status

- Request

```curl -XGET -k "https://localhost:8080/api/health"```

- Response

```Services is up and running```


### Deliver documents

- Request

```curl -XPOST --user admin:ADMINKEY -H "Content-Type: application/octet-stream" -H "Content-Disposition: attachment; filename=someFile.xml" -k "https://localhost:8080/api/files" --data-binary "@/path/to/file"```

- Response

```{"uuid":"a3569d23-83c1-42bd-ad08-1bf8974ffa6c"}```

### List documents

- Request

```curl -XGET --user admin:ADMINKEY -H "Accept: application/json" -k "https://localhost:8080/api/files?limit=5&offset=20" | json_pp ```

- Response

```
{
      "self" : {
         "href" : "https://localhost:8080/api/files?limit=5&offset=20"
      },
      "last" : {
         "href" : "https://localhost:8080/api/files?limit=5&offset=30"
      },
      "next" : {
         "href" : "https://localhost:8080/api/files?limit=5&offset=25"
      },
      "files" : [
         {
            "size" : 764,
            "name" : "spring_security_acl_mongodb.json",
            "createdAt" : "2017-08-30T10:48:56Z",
            "self" : {
               "href" : "https://localhost:8080/api/files/5fb59bb9-15dd-4254-92a3-d8ca20daec67",
               "templated" : true
            }
         },
         {
            "name" : "spring_security_acl_mongodb.json",
            "createdAt" : "2017-08-30T10:48:56Z",
            "self" : {
               "href" : "https://localhost:8080/api/files/25a7987d-76f9-4359-837b-0307ab85f1d4",
               "templated" : true
            },
            "size" : 764
         },
         {
            "size" : 764,
            "self" : {
               "href" : "https://localhost:8080/api/files/d02c55d8-b827-43ae-affb-b0a4d8c499e1",
               "templated" : true
            },
            "createdAt" : "2017-08-30T10:48:57Z",
            "name" : "spring_security_acl_mongodb.json"
         },
         {
            "createdAt" : "2017-08-30T10:48:58Z",
            "self" : {
               "templated" : true,
               "href" : "https://localhost:8080/api/files/fc083ae3-2994-4b39-8820-46ff7107a46c"
            },
            "name" : "spring_security_acl_mongodb.json",
            "size" : 764
         },
         {
            "name" : "spring_security_acl_mongodb.json",
            "self" : {
               "href" : "https://localhost:8080/api/files/3cec3700-4f4a-4f17-aea3-2b445cb625e8",
               "templated" : true
            },
            "createdAt" : "2017-08-30T10:48:59Z",
            "size" : 764
         }
      ],
      "prev" : {
         "href" : "https://localhost:8080/api/files?limit=5&offset=15"
      },
      "first" : {
         "href" : "https://localhost:8080/api/files?limit=5&offset=0"
      }
   }
```

### Document details

- Request

```curl -XGET --user admin:ADMINKEY -H "Accept: application/json" -k "https://localhost:8080/api/files/3cec3700-4f4a-4f17-aea3-2b445cb625e8" | json_pp```

- Response

```
{
   "name" : "spring_security_acl_mongodb.json",
   "content" : "ewogICJpZCI6ICJvaWQuLi4iLAogICJuYW1lIjogIkRyYXdpbmdQZXJtaXNzaW9uIiwKICAiYnVzaW5lc3NPYmplY3RzIjogWwogICAgewogICAgICAiaWQiOiAib2lkLi4uIiwKICAgICAgImNsYXNzIjogImNvbS5lY29zaW8uc3JtLi4uLiIsCiAgICAgICJpbnN0YW5jZUlkIjogIi4uLiIsCiAgICAgICJvd25lciI6ICJvaWQxMjM0IiwKICAgICAgInBhcmVudCI6ICJvaWQxMTExIiwKICAgICAgImluaGVyaXRfcGVybWlzc2lvbnMiOiB0cnVlLAogICAgICAicGVybWlzc2lvbnMiOiBbCiAgICAgICAgewogICAgICAgICAgInNpZCI6ICJvaWQxMjM1IiwKICAgICAgICAgICJwb3NpdGlvbiI6IDEsCiAgICAgICAgICAibWFzayI6IDExMDAsIC8vIHJlYWQsIHVwZGF0ZSwgY3JlYXRlLCBkZWxldGUKICAgICAgICAgICJncmFudGluZyI6IGZhbHNlLCAvLyBhbGxvd2VkIHRvIGdyYW50IHBlcm1pc3Npb25zIHRvIG90aGVycwogICAgICAgICAgImF1ZGl0X3N1Y2Nlc3MiOiBmYWxzZSwKICAgICAgICAgICJhdWRpdF9mYWlsdXJlIjogdHJ1ZQogICAgICAgIH0sCiAgICAgICAgewogICAgICAgICAgInNpZCI6ICJvaWQxMjM2IiwKICAgICAgICAgICJwb3NpdGlvbiI6IDIsCiAgICAgICAgICAibWFzayI6IDExMTEsCiAgICAgICAgICAiZ3JhbnRpbmciOiB0cnVlLAogICAgICAgICAgImF1ZGl0X3N1Y2Nlc3MiOiBmYWxzZSwKICAgICAgICAgICJhdWRpdF9mYWlsdXJlIjogdHJ1ZQogICAgICAgIH0KICAgICAgXQogICAgfSwKICAgIC4uLgogIF0KfQo=",
   "size" : 764,
   "self" : {
      "href" : "https://localhost:8080/api/files/3cec3700-4f4a-4f17-aea3-2b445cb625e8/3cec3700-4f4a-4f17-aea3-2b445cb625e8",
      "templated" : true
   },
   "createdAt" : "2017-08-30T10:48:59Z"
}
```

## Mongo View collections

This application uses MongoDB views on the user collection which load only necessary data for either the authentication process or for preventing Morphia from automatically loading referenced collections in the back on accessing certain fields, as this might avoid custom caching strategies.

The currently required views can be created as follows:

- User view used for authentication:

```db.createView( "authUserView", "user", [ { $project: { "userId": 1, "userKeyEncrypted": 1, "uuid":1, "roles": 1, "passwordHash": 1, "disabled": 1 } } ] )```

- User view which replaces MongoDB's DBRef construct with a simple UUID replacement of the referenced company

```
db.createView( "companyUserView", "user", [
       { $project: { "userId": 1, "userKeyEncrypted": 1, "uuid":1, "roles": 1, "passwordHash": 1, "disabled": 1, company: { $objectToArray: "$$ROOT.company" }} }, 
       { $unwind: "$company" }, 
       { $match: { "company.k": "$id"}  }, 
       { $lookup: { from: "company", localField: "company.v", foreignField: "_id", as: "company_data" } },
       { $project: { "userId": 1, "userKeyEncrypted": 1, "uuid":1, "roles": 1, "passwordHash": 1, "disabled": 1,  "companyUuid": { $arrayElemAt: [ "$company_data.uuid", 0 ] } } }
   ])
   ```

or 

```
db.createView( "companyUserView", "user", [
    { $project: { "userId": 1, "userKeyEncrypted": 1, "uuid":1, "roles": 1, "passwordHash": 1, "disabled": 1, companyRefs: { $let: { vars: { refParts: { $objectToArray: "$$ROOT.company" }}, in: "$$refParts.v" } } } }, 
    { $match: { "companyRefs": { $exists: true } } }, 
    { $project: { "userId": 1, "userKeyEncrypted": 1, "uuid":1, "roles": 1, "passwordHash": 1, "disabled": 1, "companyRef": { $arrayElemAt: [ "$companyRefs", 1 ] } } }, 
    { $lookup: { from: "company", localField: "companyRef", foreignField: "_id", as: "company_data" } }, 
    { $project: { "userId": 1, "userKeyEncrypted": 1, "uuid":1, "roles": 1, "passwordHash": 1, "disabled": 1,  "companyUuid": { $arrayElemAt: [ "$company_data.uuid", 0 ] } } }
])
```

## TODO:

Certain things I want to get up and running are:

- The application (Camel in specific) should adhere to the filter chain defined in the `SpringSecurityConfig` so that adding a custom servlet filter (i.e. `AWSXRayServletFilter`) can do what it is intended for
- Get hal+json responses from the services. Currently the project uses [jackson-dataformat-hal](https://github.com/Nykredit/jackson-dataformat-hal) though this requires a custom `JacksonJsonProvider` which I was not yet able to get it to work, thus the response format is not real hal+json as no `"_embedded"` or `"_links"` fields are available yet.
- File uploads should return a 201 with a location header of the URI to look up the file again (easy to fix but not yet done)



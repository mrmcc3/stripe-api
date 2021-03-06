
# stripe-api

A Stripe API Client for Clojure.

STATUS: Pre-alpha, in design and prototyping phase.

### Rationale

The Cognitect AWS API https://github.com/cognitect-labs/aws-api library provides a
data first approach to using the AWS API from Clojure. The motivation for this library
is to use a similar approach for the Stripe API. In fact we can use the same rationale 
for that library using Stripe instead of AWS

> Stripe APIs are data-oriented in both the "send data, get data back" sense, 
> and the fact that all of the operations and data structures are, themselves, 
> described in data which can be used to generate mechanical transformations 
> from application data to wire data and back. 
>
> This is exactly what we want from a Clojure API.
>
> stripe-api is an idiomatic, data-oriented Clojure library for invoking Stripe APIs. 
> While the library offers some helper and documentation functions you'll use at 
> development time, the only functions you ever need at runtime are `client`, 
> which creates a client and `invoke`, which invokes a stripe API operation. 
> `invoke` takes a map and returns a map, and works the same way for every operation.

### Approach

Stripe publishes a description of their API as data in the form of an OpenAPI Spec. 

https://github.com/stripe/openapi

Among other information The spec contains

* The Stripe API version
* All the operations along with the data requirements needed to invoke them

We have generator code (not released yet) that pulls the stripe OpenAPI spec 
and transforms it. We intend to publish the transformed spec so that you 
can use a specific version (as a dependency) alongside the stripe-api client library.

A version/packaging scheme has yet to be decided on. For now a basic spec 
is included with the stripe-api client library to get started. 

### Usage

For now use as a git dependency in `deps.edn`

```clojure
{:deps {mrmcc3/stripe-api
        {:git/url "https://github.com/mrmcc3/stripe-api" 
         :sha     "..."}}}
```

For now see `dev/user.clj`

### TODO/IDEAS

Roughly ordered by priority

* [ ] simulation/integration style tests against stripe dev api
* [ ] exploration via datafy/nav/REBL
* [ ] show output/response shape in operation doc
* [ ] auto pagination? response handling?
* [ ] support file uploads
* [ ] publish generated api specs and document usage
* [ ] improve usage docs in readme
* [ ] client only tests (not api). client.util and webhook validation
* [ ] generate clojure specs and leverage them?
      dev time request validation? example request generation?
* [ ] cli - native image?
* [ ] cljs?

Implemented

* [x] better doc for operations. include required top level params
* [x] automatic path, query and body param selection based off api spec
* [x] webhook validation
* [x] configurable http-client with default impl for Jetty, Java 11 http client, Java 8 URLConnection
* [x] public client api docstrings


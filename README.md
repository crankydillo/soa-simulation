# Introduction

Show the effect that seemingly harmless, blocking calls can do in an SOA
system.

# Usage

Start a web service on port 8080 that uses an system has 5 layers.  Each node
has 2 children.

The number of calls made to process a request is `c^l - 1` where `c` is the
children per node and `l` is the number of layers.  So in the example above,
there will be 31 calls made to process a request.

```
PORT=8080; docker run -p $PORT:$PORT crankydillo/soa-simulation $PORT 5 2
```

## Request that triggers non-blocking code

```
time curl localhost:8080/nb/req/1s
```

## Request that triggers blocking code

```
time curl localhost:8080/b/req/1s
```

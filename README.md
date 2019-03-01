# Introduction

Show the effect that seemingly harmless, sequential and/or blocking calls can
do in an SOA system.

# Usage

Start a web service on port 8080 that uses a system with 5 layers.  Each node
has 2 children.

```
PORT=8080; docker run -p $PORT:$PORT crankydillo/soa-simulation $PORT 5 2
```

The number of calls is a geometric series: `(1 - c^(l+1))/(1 - c)` where where
`c` is the number of children per node.   where `c` is the children per node
and `l` is the number of layers.  

So in using the command above, there will be 63 calls made to process a
request.

# Latency impact of sequential vs parallel

While this could have been done 2 services, where the edge just makes N calls
to the second service, the point is to show the effect when people might be
tempted do things sequentially since they are only doing 'a few things'.  If
everyone makes that same reasoning, it can have a big impact on the latency of
a layered system.

## Request that triggers parallel code

```
time curl localhost:8080/nb/req/1s
```

## Request that triggers sequential code

```
time curl localhost:8080/b/req/1s
```

# Throughput impact of blocking vs non-blocking

For this, you'll have to have SBT in order to run the gatling-powered load
test.

In SBT's REPL, use:

```
reStart 8080 5 2
gatling:test
```

You exercise the non-blocking code first because the blocking will bring the
server to its knees.

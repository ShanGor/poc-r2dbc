# 异步读数据库解决连接数不够的问题
Ref to https://www.jianshu.com/p/884ac5b1ef7a?v=1668856381215

**标签**: `R2DBC`, `out of database connections`

最近听说邻Team发生了Prod issue, 原因是DB Connection不够导致大量timeout. 我想起之前项目也有类似的问题，当时已经用了Redis, 但是并发量一大，Redis无法命中，所有请求都争抢数据库连接来读数据，于是很多线程挂起，直接内存耗尽…干得漂亮！后来时间紧，解决的办法还是调整HikariCP的参数，增加初始和 max 连接数。DB的配置也升上去。

现在想想这个问题，其实根源还是在于数据库的请求是同步的，大家在争Connection. 那有没有可能，关系型数据库也能跟MongoDB一样可以支持异步请求呢？如果是这样，那么一个连接就可以submit一大堆请求。这样的话，瓶颈就不再是这个数据库长连接，而是变成了带宽和DB的CPU/内存。至少解决了App端的问题。

于是山哥Google了一下JDBC Async Connection。嘿！没想到Spring 早就做出来了，有个叫R2DBC的产品，Exactly就是解决这个问题的！真是英雄所见略同啊！

那问题来了，它是不是真能解决这个问题呢？山哥做了个性能测试。结果很喜人！
#### 结果
- 同步方式，并发100很轻松，不过1000并发就扛不住了
- 异步方式，返回速度更快了，并发1000没问题，并发1500也没问题。限于本机配置，没试2000并发。
#### 环境：
- 小米laptop一部，windows
- postgresql 12，远程
- Tomcat在CRUD的场景，并发性一定是差的了，为了公平起见，用的都 Netty based的 Webflux.
- 同步用HikariCP, pool 连接数固定10
- 异步用R2DBC, pool 连接数固定10
- 数据库创建新表，生成纪录 125万1,043条
- 用hey做性能测试，用go写的，优点是省资源，触发并发高，缺点是参数不可变。

# 测试详情
## 传统的同步连接
我们用HikariCP这个号称最快的连接池，看看表现如何！
**Case 1**: 总请求2万，并发数100，结果是完全ok的. 最慢的是1.5秒能返回。
```
$ hey -n 20000 -c 100 http://localhost:8080/users/1000

Summary:
  Total:        99.8397 secs
  Slowest:      1.5725 secs
  Fastest:      0.0211 secs
  Average:      0.4952 secs
  Requests/sec: 200.3211

  Total data:   2200000 bytes
  Size/request: 110 bytes

Response time histogram:
  0.021 [1]     |
  0.176 [41]    |
  0.331 [61]    |
  0.487 [8009]  |■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.642 [11868] |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  0.797 [2]     |
  0.952 [4]     |
  1.107 [10]    |
  1.262 [0]     |
  1.417 [0]     |
  1.573 [4]     |


Latency distribution:
  10% in 0.4424 secs
  25% in 0.4688 secs
  50% in 0.4953 secs
  75% in 0.5255 secs
  90% in 0.5430 secs
  95% in 0.5623 secs
  99% in 0.5830 secs

Details (average, fastest, slowest):
  DNS+dialup:   0.0001 secs, 0.0211 secs, 1.5725 secs
  DNS-lookup:   0.0000 secs, 0.0000 secs, 0.0095 secs
  req write:    0.0000 secs, 0.0000 secs, 0.0038 secs
  resp wait:    0.4949 secs, 0.0209 secs, 1.5592 secs
  resp read:    0.0001 secs, 0.0000 secs, 0.0047 secs

Status code distribution:
  [200] 20000 responses
```
**Case 2**: 总请求2万，并发数100**0**，结果有3,619个清求失败！现在我们得到一个基准，就是**`它扛不住1000的并发！`**
```
hey -n 20000 -c 1000 http://localhost:8080/users/1000

Summary:
  Total:        97.8581 secs
  Slowest:      19.8605 secs
  Fastest:      0.0205 secs
  Average:      3.6470 secs
  Requests/sec: 204.3777

  Total data:   1801910 bytes
  Size/request: 110 bytes

Response time histogram:
  0.021 [1]     |
  2.005 [1591]  |■■■■■■■■
  3.989 [6131]  |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  5.973 [8182]  |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  7.957 [236]   |■
  9.941 [33]    |
  11.925 [47]   |
  13.909 [45]   |
  15.893 [46]   |
  17.877 [34]   |
  19.861 [35]   |


Latency distribution:
  10% in 2.1354 secs
  25% in 3.0965 secs
  50% in 4.0137 secs
  75% in 4.1729 secs
  90% in 4.2579 secs
  95% in 4.3879 secs
  99% in 11.5096 secs

Details (average, fastest, slowest):
  DNS+dialup:   0.0042 secs, 0.0205 secs, 19.8605 secs
  DNS-lookup:   0.0039 secs, 0.0000 secs, 0.3964 secs
  req write:    0.0003 secs, 0.0000 secs, 0.0713 secs
  resp wait:    1.8527 secs, 0.0203 secs, 19.1682 secs
  resp read:    0.0001 secs, 0.0000 secs, 0.0166 secs

Status code distribution:
  [200] 16381 responses

Error distribution:
  [891] Get "http://localhost:8080/users/1000": context deadline exceeded (Client.Timeout exceeded while awaiting headers)
  [2728]        Get "http://localhost:8080/users/1000": dial tcp [::1]:8080: connectex: No connection could be made because the target machine actively refused it.
```
## R2DBC 做异步读 DB
现在我们来看 R2DBC 的实现。
**Case 3**: 总请求2万，并发数1000，结果是完全没问题，总时长是传统版的 `1/3`。
```
$ hey -n 20000 -c 1000 http://localhost:8080/users/1000

Summary:
  Total:        32.4116 secs
  Slowest:      6.0491 secs
  Fastest:      0.0120 secs
  Average:      1.5743 secs
  Requests/sec: 617.0627

  Total data:   2200000 bytes
  Size/request: 110 bytes

Response time histogram:
  0.012 [1]     |
  0.616 [252]   |■
  1.219 [322]   |■
  1.823 [19174] |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  2.427 [244]   |■
  3.031 [5]     |
  3.634 [0]     |
  4.238 [1]     |
  4.842 [0]     |
  5.445 [0]     |
  6.049 [1]     |


Latency distribution:
  10% in 1.5245 secs
  25% in 1.5659 secs
  50% in 1.5957 secs
  75% in 1.6300 secs
  90% in 1.6574 secs
  95% in 1.7025 secs
  99% in 1.8993 secs

Details (average, fastest, slowest):
  DNS+dialup:   0.0021 secs, 0.0120 secs, 6.0491 secs
  DNS-lookup:   0.0017 secs, 0.0000 secs, 0.1242 secs
  req write:    0.0002 secs, 0.0000 secs, 0.0994 secs
  resp wait:    1.5660 secs, 0.0118 secs, 6.0490 secs
  resp read:    0.0000 secs, 0.0000 secs, 0.0048 secs

Status code distribution:
  [200] 20000 responses
```

**Case 4**: 10万请求，并发数1000, 还是全部请求很快处理完，最慢返回是3.95秒！
```
hey -n 100000 -c 1000 http://localhost:8080/users/10001

Summary:
  Total:        168.8792 secs
  Slowest:      3.9542 secs
  Fastest:      0.1268 secs
  Average:      1.6802 secs
  Requests/sec: 592.1392

  Total data:   11100000 bytes
  Size/request: 111 bytes

Response time histogram:
  0.127 [1]     |
  0.510 [203]   |
  0.892 [234]   |
  1.275 [222]   |
  1.658 [44698] |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  2.041 [53637] |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  2.423 [5]     |
  2.806 [0]     |
  3.189 [0]     |
  3.572 [5]     |
  3.954 [995]   |■


Latency distribution:
  10% in 1.5716 secs
  25% in 1.6229 secs
  50% in 1.6649 secs
  75% in 1.7037 secs
  90% in 1.7487 secs
  95% in 1.7995 secs
  99% in 3.4129 secs

Details (average, fastest, slowest):
  DNS+dialup:   0.0008 secs, 0.1268 secs, 3.9542 secs
  DNS-lookup:   0.0004 secs, 0.0000 secs, 0.1074 secs
  req write:    0.0000 secs, 0.0000 secs, 0.0166 secs
  resp wait:    1.6793 secs, 0.0735 secs, 3.9542 secs
  resp read:    0.0001 secs, 0.0000 secs, 0.0036 secs

Status code distribution:
  [200] 100000 responses
```

**Case 5**: 10万请求，并发数1500，可以看到也是完全扛住了，只是因为并发数是1500, 总数只能到达99000个请求！但没有出错和timeout的！
```
$ hey -n 100000 -c 1500 http://localhost:8080/users/1000

Summary:
  Total:        187.2399 secs
  Slowest:      5.5965 secs
  Fastest:      1.5804 secs
  Average:      2.8152 secs
  Requests/sec: 528.7335

  Total data:   10890000 bytes
  Size/request: 110 bytes

Response time histogram:
  1.580 [1]     |
  1.982 [77]    |
  2.384 [153]   |
  2.785 [53772] |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  3.187 [42372] |■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
  3.588 [1295]  |■
  3.990 [562]   |
  4.392 [177]   |
  4.793 [197]   |
  5.195 [199]   |
  5.597 [195]   |


Latency distribution:
  10% in 2.7003 secs
  25% in 2.7369 secs
  50% in 2.7761 secs
  75% in 2.8391 secs
  90% in 2.9193 secs
  95% in 2.9785 secs
  99% in 3.7960 secs

Details (average, fastest, slowest):
  DNS+dialup:   0.0112 secs, 1.5804 secs, 5.5965 secs
  DNS-lookup:   0.0006 secs, 0.0000 secs, 0.0978 secs
  req write:    0.0002 secs, 0.0000 secs, 0.2837 secs
  resp wait:    2.8036 secs, 1.5178 secs, 4.4561 secs
  resp read:    0.0001 secs, 0.0000 secs, 0.0135 secs

Status code distribution:
  [200] 99000 responses
```
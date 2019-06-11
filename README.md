
# Custom UUID Generators - serial style

This project implements custom UUID version 1 (based on time and space)
generators but violates the [standard][1] in the sense that the bytes
representing the timestamp are produced different from the standard.

However, all custom implementations still preserve the version bits and systems
(inlcuding the `java.util.UUID#version()` function) will report the value as a
valid version 1 UUID. However, attempting to extract the timestamp value with
standard tools/libraries will simply fail or produce wrong values as there is
no way to tell them how to interpret the byte order.

In addition, no information is lost as these implementations just re-order the
existing bytes and hence, they still provide the exact same guarantees as a
standard version 1 UUID.


## SerialTimeBasedGenerator

For example the following is a standard version 1 UUID:
```
1004cd50-4241-11e9-b3ab-e03f49ee7ef3
```

Using the `SerialTimeBasedGenerator` (without the shift parameter) would
generate the following instead:
```
1e942411-004c-1d50-b3ab-e03f49ee7ef3
```

As time goes by, the representation of a standard V1 UUID looks pretty random
at the first bytes due to the shuffling:
```
1004cd50-4241-11e9-b3ab-e03f49ee7ef3
...
05602550-8a8c-11e9-b3ab-e03f49ee7ef3
```

...whereas the serial version will grow (in value) constantly with time (per
node):
```
1e942411-004c-1d50-b3ab-e03f49ee7ef3
...
1e98a8c0-5602-1550-b3ab-e03f49ee7ef3
```

### Shifting

Using the constructor with the additional `shift` parameter, you can tell it
to shift the timestamp value to the right and wrap around.

Examples for direct comparison:
```
  default: 1e942411-004c-1d50-b3ab-e03f49ee7ef3
shift = 1: 501e9424-1100-14cd-b3ab-e03f49ee7ef3
shift = 2: cd501e94-2411-1004-b3ab-e03f49ee7ef3
shift = 4: 1004cd50-1e94-1241-b3ab-e03f49ee7ef3
```

They all encode the exact same timestamp but in different representations.


## ReversedTimeBasedGenerator

The `ReversedTimeBasedGenerator` is basically just a special-purpose generator
that takes the timestamp bytes and reverses the byte order.

An example for comparison:
```
standard: 1004cd50-4241-11e9-b3ab-e03f49ee7ef3
  serial: 1e942411-004c-1d50-b3ab-e03f49ee7ef3
 reverse: 50cd0410-4142-1e91-b3ab-e03f49ee7ef3
```


# Credits

I would like to thank @tvondra for bringing the idea for this implementation to
my attention with his very enlightening article [Sequential UUID Generators][2].


[1]: https://tools.ietf.org/html/rfc4122
[2]: https://www.2ndquadrant.com/en/blog/sequential-uuid-generators/

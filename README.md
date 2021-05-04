### spawn time
spawn n isolates in loop

`du -h target/graalisolatehello`
=> 9.1M

`hyperfine --parameter-scan isolateCount 0 10000 -D 100 './graalisolatehello {isolateCount}'`

| Command | Mean [ms] | Min [ms] | Max [ms] | Relative |
|:---|---:|---:|---:|---:|
| `./graalisolatehello 0` | 3.6 ± 0.3 | 3.2 | 4.9 | 1.00 |
| `./graalisolatehello 1000` | 5505.6 ± 14.6 | 5487.9 | 5523.7 | 1531.09 ± 133.71 |
| `./graalisolatehello 2000` | 20656.9 ± 73.6 | 20570.2 | 20806.8 | 5744.54 ± 501.85 |
| `./graalisolatehello 3000` | 45463.5 ± 313.2 | 45229.1 | 46054.7 | 12643.10 ± 1107.03 |
| `./graalisolatehello 4000` | 79714.0 ± 242.5 | 79469.4 | 80227.9 | 22167.95 ± 1936.19 |
| `./graalisolatehello 5000` | 123621.2 ± 673.2 | 123001.5 | 124851.5 | 34378.27 ± 3006.67 |

#### Summary
'./graalisolatehello 0' ran 
1531.09 ± 133.71 times faster than './graalisolatehello 1000' 
5744.54 ± 501.85 times faster than './graalisolatehello 2000' 
12643.10 ± 1107.03 times faster than './graalisolatehello 3000' 
22167.95 ± 1936.19 times faster than './graalisolatehello 4000' 
34378.27 ± 3006.67 times faster than './graalisolatehello 5000'

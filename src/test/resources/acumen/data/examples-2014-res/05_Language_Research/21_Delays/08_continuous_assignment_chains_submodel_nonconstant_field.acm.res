#0 {
  className = Main,
  s1 = #0.1,
  simulator = #0.0,
  x10 = 0,
  x6 = 0,
  x7 = 0,
  x8 = 0,
  x9 = 0
}
#0.0 { className = Simulator, time = 0.000000 }
#0.1 {
  className = S1,
  s2 = #0.1.0,
  s2x2a = 0,
  x3 = 0,
  x4 = 0,
  x5 = 0
}
#0.1.0 {
  className = S2,
  x0 = 2,
  x0' = 0,
  x0'' = 0,
  x1 = 0,
  x2 = 0
}
------------------------------0
#0 {
  className = Main,
  s1 = #0.1,
  simulator = #0.0,
  x10 = 7.828456,
  x6 = 9.214108,
  x7 = 8.933470,
  x8 = 8.608711,
  x9 = 8.240136
}
#0.0 { className = Simulator, time = 10.010000 }
#0.1 {
  className = S1,
  s2 = #0.1.0,
  s2x2a = 9.793814,
  x3 = 9.793814,
  x4 = 9.643687,
  x5 = 9.450704
}
#0.1.0 {
  className = S2,
  x0 = -26.747910,
  x0' = -5.998988,
  x0'' = 9.902127,
  x1 = -26.687425,
  x2 = -26.625956
}
------------------------------1001
SOME HYPOTHESES FALSIFIED OVER [0.0..10.009999999999831]
0 TRUE, 1 FALSE, 0 INCONCLUSIVE
- (#0:Main) 'continuous assignments do not cause delays' Falsified at 0.02, where x10 = 0, self.s1.s2x2a = -0.0


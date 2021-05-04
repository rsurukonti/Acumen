#0 {
  _3D = (("Text", (-1.500000, 0.200000, -2), 0.500000, (1.600000, 1.600000, 0.500000), (-0.300000, 0, -0.300000), 1, "Global", -1.000000), ("Text", (-0.500000, 0, -2), 0.300000, (1.600000, 1.600000, 0.100000), (-0.300000, 0, -0.300000), "Falling Ball", "Global", -1.000000)),
  caption = "Falling Ball",
  className = Main,
  e = #0.1,
  example = 1,
  exampleTime = 5,
  mode = "1Continue",
  simulator = #0.0,
  t = 0,
  t' = 1,
  type = Main
}
#0.0 { className = Simulator, time = 0.000000 }
#0.1 {
  D = (0, 0, 0),
  className = example_1,
  flag = 0,
  m = #0.1.0,
  type = example_1
}
#0.1.0 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = -98.000000,
  flag = 0,
  m = 10,
  p = 3,
  p' = 0,
  p'' = -9.800000,
  p0 = 3,
  s = #0.1.0.0,
  type = mass_1d
}
#0.1.0.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, 3.000000), 0.094868, (3.333333, 1.455979, -3.000000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 10,
  p = (0, 0, 3),
  type = sphere
}
------------------------------0
#0 {
  _3D = (("Text", (-1.500000, 0.200000, -2), 0.500000, (1.600000, 1.600000, 0.500000), (-0.300000, 0, -0.300000), 2, "Global", -1.000000), ("Text", (-0.500000, 0, -2), 0.300000, (1.600000, 1.600000, 0.100000), (-0.300000, 0, -0.300000), "2Mass 1Spring", "Global", -1.000000)),
  caption = "2Mass 1Spring",
  className = Main,
  e = #0.2,
  example = 2,
  exampleTime = 5,
  mode = "2Continue",
  simulator = #0.0,
  t = 0,
  t' = 1,
  type = Main
}
#0.0 { className = Simulator, time = 5.015625 }
#0.2 {
  D = (0, 0, 0),
  className = example_2,
  flag = 0,
  m1 = #0.2.0,
  m2 = #0.2.1,
  s = #0.2.2,
  type = example_2
}
#0.2.0 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = -3.750000,
  flag = 0,
  m = 15,
  p = 1,
  p' = 0,
  p'' = -0.250000,
  p0 = 1,
  s = #0.2.0.0,
  type = mass_1d
}
#0.2.0.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, 1.000000), 0.116190, (5.000000, 2.650288, -5.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 15,
  p = (0, 0, 1),
  type = sphere
}
#0.2.1 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = 3.750000,
  flag = 0,
  m = 5,
  p = -1,
  p' = 0,
  p'' = 0.750000,
  p0 = -1,
  s = #0.2.1.0,
  type = mass_1d
}
#0.2.1.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, -1.000000), 0.067082, (1.666667, 1.041076, -0.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 5,
  p = (0, 0, -1),
  type = sphere
}
#0.2.2 {
  D = (0, 0, 0),
  c = #0.2.2.0,
  className = spring_1d,
  e_p = 1.406250,
  f1 = -3.750000,
  f2 = 3.750000,
  flag = 0,
  k = 5,
  l = 1.250000,
  p1 = 1,
  p2 = -1,
  type = spring_1d
}
#0.2.2.0 {
  D = (0, 0, 0),
  _3D = ("Cylinder", (0.000000, 0.000000, 0.000000), (0.010000, 2.000000), (1, 1, 1), (1.570796, 0, -3.141593), "Global", -1.000000),
  alpha = 1.570796,
  className = cylinder,
  flag = 0,
  l = 2.000000,
  p = (0, 0, 1),
  q = (0, 0, -1),
  r = 0.010000,
  theta = 3.141593,
  type = cylinder,
  x = 0.000000,
  y = 0.000000,
  z = 2.000000
}
------------------------------322
#0 {
  _3D = (("Text", (-1.500000, 0.200000, -2), 0.500000, (1.600000, 1.600000, 0.500000), (-0.300000, 0, -0.300000), 3, "Global", -1.000000), ("Text", (-0.500000, 0, -2), 0.300000, (1.600000, 1.600000, 0.100000), (-0.300000, 0, -0.300000), "3Mass 2Spring", "Global", -1.000000)),
  caption = "3Mass 2Spring",
  className = Main,
  e = #0.3,
  example = 3,
  exampleTime = 5,
  mode = "3Continue",
  simulator = #0.0,
  t = 0,
  t' = 1,
  type = Main
}
#0.0 { className = Simulator, time = 10.031250 }
#0.3 {
  D = (0, 0, 0),
  b = #0.3.5,
  className = example_3,
  flag = 0,
  m1 = #0.3.0,
  m2 = #0.3.1,
  m3 = #0.3.2,
  s1 = #0.3.3,
  s2 = #0.3.4,
  type = example_3
}
#0.3.0 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = -1.250000,
  flag = 0,
  m = 15,
  p = 1,
  p' = 0,
  p'' = -0.083333,
  p0 = 1,
  s = #0.3.0.0,
  type = mass_1d
}
#0.3.0.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, 1.000000), 0.116190, (5.000000, 2.650288, -5.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 15,
  p = (0, 0, 1),
  type = sphere
}
#0.3.1 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = 1.250000,
  flag = 0,
  m = 5,
  p = -1,
  p' = 0,
  p'' = 0.250000,
  p0 = -1,
  s = #0.3.1.0,
  type = mass_1d
}
#0.3.1.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, -1.000000), 0.067082, (1.666667, 1.041076, -0.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 5,
  p = (0, 0, -1),
  type = sphere
}
#0.3.2 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = -0.000000,
  flag = 0,
  m = 1,
  p = -1.500000,
  p' = 0,
  p'' = -0.000000,
  p0 = -1.500000,
  s = #0.3.2.0,
  type = mass_1d
}
#0.3.2.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, -1.500000), 0.030000, (0.333333, 2.841471, 1.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 1,
  p = (0, 0, -1.500000),
  type = sphere
}
#0.3.3 {
  D = (0, 0, 0),
  c = #0.3.3.0,
  className = spring_1d,
  e_p = 0.156250,
  f1 = -1.250000,
  f2 = 1.250000,
  flag = 0,
  k = 5,
  l = 1.750000,
  p1 = 1,
  p2 = -1,
  type = spring_1d
}
#0.3.3.0 {
  D = (0, 0, 0),
  _3D = ("Cylinder", (0.000000, 0.000000, 0.000000), (0.010000, 2.000000), (1, 1, 1), (1.570796, 0, -3.141593), "Global", -1.000000),
  alpha = 1.570796,
  className = cylinder,
  flag = 0,
  l = 2.000000,
  p = (0, 0, 1),
  q = (0, 0, -1),
  r = 0.010000,
  theta = 3.141593,
  type = cylinder,
  x = 0.000000,
  y = 0.000000,
  z = 2.000000
}
#0.3.4 {
  D = (0, 0, 0),
  c = #0.3.4.0,
  className = spring_1d,
  e_p = 0.000000,
  f1 = 0.000000,
  f2 = -0.000000,
  flag = 0,
  k = 5,
  l = 0.500000,
  p1 = -1,
  p2 = -1.500000,
  type = spring_1d
}
#0.3.4.0 {
  D = (0, 0, 0),
  _3D = ("Cylinder", (0.000000, 0.000000, -1.250000), (0.010000, 0.500000), (1, 1, 1), (1.570796, 0, -3.141593), "Global", -1.000000),
  alpha = 1.570796,
  className = cylinder,
  flag = 0,
  l = 0.500000,
  p = (0, 0, -1),
  q = (0, 0, -1.500000),
  r = 0.010000,
  theta = 3.141593,
  type = cylinder,
  x = 0.000000,
  y = 0.000000,
  z = 0.500000
}
#0.3.5 {
  D = (0, 0, 0),
  _3D = ("Cylinder", (0.000000, 0.200000, 0.937500), (0.020000, 1.875000), (0.100000, 3, 0.100000), (-1.570796, 0, 0), "Global", -1.000000),
  c = (0.100000, 3, 0.100000),
  className = display_bar,
  flag = 0,
  type = display_bar,
  v = 1.875000
}
------------------------------644
#0 {
  _3D = (("Text", (-1.500000, 0.200000, -2), 0.500000, (1.600000, 1.600000, 0.500000), (-0.300000, 0, -0.300000), 4, "Global", -1.000000), ("Text", (-0.500000, 0, -2), 0.300000, (1.600000, 1.600000, 0.100000), (-0.300000, 0, -0.300000), "P Control", "Global", -1.000000)),
  caption = "P Control",
  className = Main,
  e = #0.4,
  example = 4,
  exampleTime = 5,
  mode = "4Continue",
  simulator = #0.0,
  t = 0,
  t' = 1,
  type = Main
}
#0.0 { className = Simulator, time = 15.046875 }
#0.4 {
  D = (0, 0, 0),
  b = #0.4.6,
  c = #0.4.5,
  className = example_4,
  flag = 0,
  m1 = #0.4.0,
  m2 = #0.4.1,
  m3 = #0.4.2,
  s1 = #0.4.3,
  s2 = #0.4.4,
  type = example_4
}
#0.4.0 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = -1.500000,
  flag = 0,
  m = 15,
  p = 1,
  p' = 0,
  p'' = -0.100000,
  p0 = 1,
  s = #0.4.0.0,
  type = mass_1d
}
#0.4.0.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, 1.000000), 0.116190, (5.000000, 2.650288, -5.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 15,
  p = (0, 0, 1),
  type = sphere
}
#0.4.1 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = 1.250000,
  flag = 0,
  m = 5,
  p = -1,
  p' = 0,
  p'' = 0.250000,
  p0 = -1,
  s = #0.4.1.0,
  type = mass_1d
}
#0.4.1.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, -1.000000), 0.067082, (1.666667, 1.041076, -0.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 5,
  p = (0, 0, -1),
  type = sphere
}
#0.4.2 {
  D = (0, 0, 0),
  className = mass_1d,
  e_k = 0.000000,
  f = 0.250000,
  flag = 0,
  m = 1,
  p = -1.500000,
  p' = 0,
  p'' = 0.250000,
  p0 = -1.500000,
  s = #0.4.2.0,
  type = mass_1d
}
#0.4.2.0 {
  D = (0, 0, 0),
  _3D = ("Sphere", (0.000000, 0.000000, -1.500000), 0.030000, (0.333333, 2.841471, 1.500000), (1, 1, 1), "Global", -1.000000),
  className = sphere,
  flag = 0,
  m = 1,
  p = (0, 0, -1.500000),
  type = sphere
}
#0.4.3 {
  D = (0, 0, 0),
  c = #0.4.3.0,
  className = spring_1d,
  e_p = 0.156250,
  f1 = -1.250000,
  f2 = 1.250000,
  flag = 0,
  k = 5,
  l = 1.750000,
  p1 = 1,
  p2 = -1,
  type = spring_1d
}
#0.4.3.0 {
  D = (0, 0, 0),
  _3D = ("Cylinder", (0.000000, 0.000000, 0.000000), (0.010000, 2.000000), (1, 1, 1), (1.570796, 0, -3.141593), "Global", -1.000000),
  alpha = 1.570796,
  className = cylinder,
  flag = 0,
  l = 2.000000,
  p = (0, 0, 1),
  q = (0, 0, -1),
  r = 0.010000,
  theta = 3.141593,
  type = cylinder,
  x = 0.000000,
  y = 0.000000,
  z = 2.000000
}
#0.4.4 {
  D = (0, 0, 0),
  c = #0.4.4.0,
  className = spring_1d,
  e_p = 0.000000,
  f1 = 0.000000,
  f2 = -0.000000,
  flag = 0,
  k = 5,
  l = 0.500000,
  p1 = -1,
  p2 = -1.500000,
  type = spring_1d
}
#0.4.4.0 {
  D = (0, 0, 0),
  _3D = ("Cylinder", (0.000000, 0.000000, -1.250000), (0.010000, 0.500000), (1, 1, 1), (1.570796, 0, -3.141593), "Global", -1.000000),
  alpha = 1.570796,
  className = cylinder,
  flag = 0,
  l = 0.500000,
  p = (0, 0, -1),
  q = (0, 0, -1.500000),
  r = 0.010000,
  theta = 3.141593,
  type = cylinder,
  x = 0.000000,
  y = 0.000000,
  z = 0.500000
}
#0.4.5 {
  className = controller_p1,
  f = -0.250000,
  g = 2.250000,
  k_p = 1,
  type = controller_p1,
  v = 2.500000
}
#0.4.6 {
  D = (0, 0, 0),
  _3D = ("Cylinder", (0.000000, 0.200000, 0.937500), (0.020000, 1.875000), (0.100000, 3, 0.100000), (-1.570796, 0, 0), "Global", -1.000000),
  c = (0.100000, 3, 0.100000),
  className = display_bar,
  flag = 0,
  type = display_bar,
  v = 1.875000
}
------------------------------966

------------------------------4532
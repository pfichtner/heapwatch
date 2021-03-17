assert new File(basedir, 'build.log').exists()
def log = new File( basedir, 'build.log').text
assert log.contains("heapSpace")
assert log.contains("not a value greater than <0B>")
assert log.contains("was <914432K>")

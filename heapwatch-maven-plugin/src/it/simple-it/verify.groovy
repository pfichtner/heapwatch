assert new File(basedir, 'build.log').exists()
def log = new File( basedir, 'build.log').text
assert log.contains("value less than <1G> matched for value <914432K>");

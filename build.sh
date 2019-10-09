javac -d ./build src/de/huckit/cmdtodos/*.java;
cd build || exit;
jar -cvfm cmdtodos.jar ../manifest.mf de/huckit/cmdtodos/*.class;
java -jar cmdtodos.jar;

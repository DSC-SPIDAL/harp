ubuntu:
	g++ main.cpp impurity.cpp tuple.cpp boosted_classify.cpp forest.cpp -o tree -lboost_thread-mt -lpthread	
mac:
	g++ main.cpp impurity.cpp tuple.cpp boosted_classify.cpp forest.cpp -o tree -I/opt/local/include/ -L/opt/local/lib -lboost_thread-mt
fedora:
	g++ main.cpp impurity.cpp tuple.cpp boosted_classify.cpp forest.cpp -o tree -lboost_thread -lpthread
icc:
	icc main.cpp impurity.cpp tuple.cpp boosted_classify.cpp forest.cpp -o tree -lboost_thread -lpthread -static -fast

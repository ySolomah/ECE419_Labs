# variables are:
# cached/not cached
# concurrency
# put ratio
# total requests - will keep constant at 10000

ratio=(0.99 0.9 0.8 0.7 0.6 0.5 0.4 0.3 0.2 0.1 0.01)
total_requests=10000
concurrency=(1 10 20 30 40 50 100)
cache=(0 10)
cache_scheme=("FIFO" "LFU" "LRU")
server="localhost"
port=50000

for cache_method in "${cache_scheme[@]}"
do
    for cache_size in "${cache[@]}"
    do
        # start up the server
        java -jar m1-server.jar $port $cache_size $cache_scheme &
        server_pid=$!
        for r in "${ratio[@]}"
        do
            for c in "${concurrency[@]}"
            do
                java -jar m1-stress.jar $server $port $total_requests $c $r > stress_results/stress_test_${r}_${c}_${cache_size}_${cache_method}.txt
            done
        done
        kill $server_pid
    done
done


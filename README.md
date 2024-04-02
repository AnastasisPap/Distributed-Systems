## TODO

Master:
- Initialize workers from master and remove executor

User:
- Book rooms (Make sure that no two users can book a room at overlapping dates at the same time)

Optimizations:
- Use hashmap for rooms in each worker with key=ID, value=Room object
- Asynchronously return the filtered rooms
# Simplechat NIO

A simple java server using Socket Selectors that:

    - handles client logins (only using an ID, no password),
    - receives messages from clients and broadcasts them to all logged-in clients,
    - handles client logouts,
    - stores all responses to client requests in a log kept in memory (not using the file system).
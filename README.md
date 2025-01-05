# Reliable One-Way Transport Protocol: UDP with ECC

## Use Case

* **One-way Data Transfer in Isolated Networks:** A power supply company needs to manage a critical nuclear reactor system. They require monitoring data from the reactor but must also prevent external data from entering. They employ specialized hardware to enforce one-way communication and encryption. Traditional one-way transport protocols (like UDP) are unreliable. Our protocol addresses this by providing a more dependable solution.

* **Communication in High-Loss, High-Error, or High-Latency Networks:** Conventional protocols like TCP or rUDP retransmit lost or corrupted packets. In unreliable networks, frequent retransmissions can significantly degrade system performance, especially when high latency is also present. Since network conditions are controlled by telecommunications providers, the key to improving performance is to reduce retransmissions. This requires making one-way transport more reliable, which is precisely what our protocol aims to achieve.

## How It Works?

### One-Way Protocol

Our protocol is built upon UDP. Messages are divided into 1024-byte packets to conform to network MTU limits. To increase throughput, packets are sent in parallel and reassembled at the receiver using message IDs and sequence IDs. This implementation utilizes Java 21, leveraging built-in non-blocking concurrent data structures, virtual threads for IO-intensive and blocking tasks, and OS threads for CPU-intensive tasks.

Currently, the only supported message type is a file, and the metadata is limited to the filename. The maximum file size after GZIP compression is 32KB.

### Reliable Protocol

Our protocol ensures reliability through a combination of Error Correcting Codes (ECC), checksums, and compression.

**Details:**

* **GZIP Compression:** Reducing data size leads to faster network transfers, minimizes packet loss caused by full router buffers, and may even reduce packet errors.

* **Hamming Code (Local Error Correction):** The probability of packet errors is typically low. Using Hamming Code further reduces this chance with minimal computational overhead and data redundancy.

* **MD5 Checksum:** If Hamming Code cannot correct a packet error, we detect and discard the corrupted packet.

* **Reed-Solomon Code (Global Error Correction):** Packet loss is more common than packet errors. If Hamming Code cannot correct the packet, it is also considered packet lost. Reed-Solomon Code (erasure code) reconstructs lost packets if enough packets from the same message are received. Although there are paper describes Reed-Solomon erasure codes with O(NlogN) complexity, our current implementation uses polynomial interpolation, resulting in O(Nlog<sup>2</sup>N) complexity.

<!-- -->

More details (Vietnamese slide): [https://docs.google.com/presentation/d/1hx6VD0BnZ56sxZWkHDUv7O3jkKbw9FzKraP-6YrnELk/edit?usp=sharing](https://docs.google.com/presentation/d/1hx6VD0BnZ56sxZWkHDUv7O3jkKbw9FzKraP-6YrnELk/edit?usp=sharing)

## Will do if have time

* Implement a faster Reed-Solomon algorithm: O(NlogN) complexity.
* Remove the message size limit.
* Support additional message types and more structured metadata.

# MongoDB Integration

The engine runs **database-free by default**. A repository abstraction (`PositionRepository`) lets the engine stay demoable with no external dependencies, while a drop-in MongoDB implementation persists game records and per-position analysis when configured.

## Repository Abstraction

```java
public interface PositionRepository {
    void saveGame(GameRecord record);
    Optional<GameRecord> loadGame(String gameId);
    List<GameRecord> listGames();
    void savePositionAnalysis(PositionAnalysis analysis);
    Optional<PositionAnalysis> loadAnalysis(String positionKey);
}
```

Two implementations:

- `NoOpPositionRepository` — the default. All methods are no-ops that log a debug message. The engine runs without any database configured.
- `MongoPositionRepository` — full MongoDB-backed implementation using `mongodb-driver-sync`.

## Configuration

The repository is selected at engine startup via `PersistenceConfig`:

1. Check the `CHESS_MONGO_URI` environment variable. If set, instantiate `MongoPositionRepository` with that connection string.
2. Else, check `config.properties` for `mongo.uri`. If present, use it.
3. Else, fall back to `NoOpPositionRepository`.

Example `config.properties`:

```properties
mongo.uri=mongodb://localhost:27017
mongo.database=chess_engine
mongo.collection.games=games
mongo.collection.analysis=analysis
```

## Local MongoDB Setup

### Prerequisites

- JDK 17
- Maven 3.6+
- MongoDB 6.0+ (Community Edition is fine)

### Install and Start MongoDB

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install -y mongodb-org
sudo systemctl start mongod
sudo systemctl enable mongod
```

**macOS (Homebrew):**
```bash
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

**Windows:** Download the MSI from mongodb.com, install, and run as a service.

### Verify

```bash
mongosh
> use chess_engine
switched to db chess_engine
> db.runCommand({ ping: 1 })
{ ok: 1 }
```

### Configure the Engine

```bash
export CHESS_MONGO_URI="mongodb://localhost:27017"
java -jar target/chess-engine-0.1.0.jar
```

The engine logs `MongoPositionRepository connected: localhost:27017/chess_engine` on startup if the connection succeeds, or falls back to `NoOpPositionRepository` with a warning if it fails.

## MongoDB Atlas Setup (Cloud)

### Step 1: Create a Cluster

1. Sign up at [mongodb.com/atlas](https://www.mongodb.com/cloud/atlas) (free tier).
2. Create a project, then a free M0 cluster (512MB storage, sufficient for analysis storage).
3. Choose a cloud region close to your deployment.

### Step 2: Configure Access

1. **Database User**: Database Access → Add new user → role `Read and write to database` → set a username and password.
2. **Network Access**: Network Access → Add IP Address → either your current IP or `0.0.0.0/0` (allow from anywhere — not recommended for production, fine for a portfolio demo).

### Step 3: Get the Connection String

Click "Connect" → "Drivers" → Java → copy the connection string:

```
mongodb+srv://<username>:<password>@cluster0.abcde.mongodb.net/?retryWrites=true&w=majority
```

### Step 4: Configure the Engine

```bash
export CHESS_MONGO_URI="mongodb+srv://<username>:<password>@cluster0.abcde.mongodb.net/?retryWrites=true&w=majority"
java -jar target/chess-engine-0.1.0.jar
```

## Document Schema

### `games` collection

```json
{
  "_id": "game-2026-06-21-abc123",
  "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
  "moves": ["e2e4", "e7e5", "g1f3", "b8c6", "f1b5"],
  "result": "1-0",
  "engineColor": "white",
  "searchInfo": [
    {"moveNumber": 1, "depth": 5, "score": 23, "pv": ["e2e4"]},
    {"moveNumber": 2, "depth": 5, "score": -15, "pv": ["e7e5"]}
  ],
  "createdAt": {"$date": "2026-06-21T12:34:56Z"}
}
```

### `analysis` collection

```json
{
  "_id": 1234567890123456789,
  "positionKey": "a1b2c3...",   // hex of the Zobrist key
  "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
  "depth": 7,
  "score": -15,
  "bestMove": "e7e5",
  "pv": ["e7e5", "g1f3", "b8c6"],
  "analyzedAt": {"$date": "2026-06-21T12:34:56Z"}
}
```

## Querying Examples

In `mongosh`:

```javascript
use chess_engine

// Find all games the engine lost
db.games.find({ result: "0-1", engineColor: "white" })

// Find positions analyzed to depth 20 or higher
db.analysis.find({ depth: { $gte: 20 } })

// Find the most common opening moves
db.games.aggregate([
  { $project: { firstMove: { $arrayElemAt: ["$moves", 0] } } },
  { $group: { _id: "$firstMove", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
])
```

## Dependencies

The MongoDB driver is declared `optional` in `pom.xml`:

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>4.10.2</version>
    <optional>true</optional>
</dependency>
```

This means the default build does **not** include the driver. To enable MongoDB support, build with the `mongo` profile:

```bash
mvn clean package -Pmongo
```

Or add the dependency manually to your runtime classpath.

## Troubleshooting

### "Connection refused"

- MongoDB isn't running. Start the service (`sudo systemctl start mongod`).
- The port is blocked by a firewall. Test with `mongosh "mongodb://localhost:27017"`.
- For Atlas: the IP isn't whitelisted. Add your IP in Atlas → Network Access.

### "Authentication failed"

- Atlas cluster requires a database user. Verify the username and password in the connection string.
- The user doesn't have the right role. Set role to `Read and write to database` at minimum.

### Engine silently falls back to NoOp

- The engine catches MongoDB connection errors and falls back to `NoOpPositionRepository`. Look for the warning log `MongoPositionRepository unavailable, falling back to NoOp: <error message>`.
- Verify the `CHESS_MONGO_URI` environment variable is actually set in the shell where the engine runs (`echo $CHESS_MONGO_URI`).

### Tests skipped in CI

`MongoPositionRepositoryTest` is guarded by `Assumptions.assumeTrue(connectionAvailable)`. Without MongoDB reachable, the test is marked skipped (not failed). To run it locally, ensure MongoDB is running on `localhost:27017` or set `CHESS_MONGO_URI`.

## Why MongoDB (for Interview Discussion)

The repository abstraction decouples persistence from the engine pipeline. MongoDB was chosen for the template because:

1. **Document model fits game records**: a game's move list, search info, and metadata form a natural document; no joins needed.
2. **Schema flexibility**: as the analysis schema evolves (adding opening tags, eval comments, etc.), no migration is required.
3. **Atlas free tier**: zero-cost cloud hosting for an interview portfolio demo.

The same repository interface would accommodate PostgreSQL, a flat JSON file, or in-process memory if the engine's needs change.

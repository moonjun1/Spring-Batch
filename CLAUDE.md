# Claude Code Instructions

## Build & Test Commands
```bash
# Build project
./gradlew build

# Run tests
./gradlew test

# Run application
./gradlew bootRun
```

## Lint & Format Commands
```bash
# Check code style
./gradlew checkstyleMain

# Format code (if available)
./gradlew spotlessApply
```

## Project Structure
- **Main Application**: `src/main/java/com/springbatch/SpringBatchApplication.java`
- **Test Files**: `src/test/java/com/springbatch/`
- **Resources**: `src/main/resources/`
- **Build Config**: `build.gradle`

## Spring Batch MVP Implementation Tasks

### Core Components to Implement
1. **Domain Models**: Person entity and PersonDto for CSV mapping
2. **Batch Configuration**: Job and Step definitions with ItemReader/Processor/Writer
3. **CSV Processing**: FlatFileItemReader for CSV input
4. **Data Processing**: ItemProcessor for data transformation
5. **Database Storage**: JpaItemWriter for persistence
6. **Test Data**: sample-data.csv in resources

### Dependencies Required
- spring-boot-starter-batch
- spring-boot-starter-data-jpa  
- h2database (runtime)

### Configuration Notes
- H2 in-memory database for testing
- Batch schema auto-initialization enabled
- H2 console available at `/h2-console`

## Testing & Verification
- Console logs show processing progress
- H2 console for database verification
- Batch metadata tables track execution history
- Check `PERSON` and `BATCH_JOB_EXECUTION` tables
# Spring Wiki AI

A collection of AI and Spring Boot demonstration projects, showcasing various technologies and use cases.

## Project Overview

This repository contains multiple demonstration projects that explore different aspects of AI integration with Spring Boot applications, workflow engines, caching strategies, and more.

## Available Demos

### AI and LLM Related

- **ai-agent-demo** - A Python-based AI agent demonstration
- **airag-demo** - RAG (Retrieval-Augmented Generation) implementation
- **deepseek-rag-demo** - DeepSeek model integration with RAG
- **dflash-demo** - DFlash (Block Diffusion for Flash Speculative Decoding) acceleration for LLMs
- **hugging-face-embeddings-demo** - Hugging Face embeddings integration
- **langchain4j-demo** - LangChain4j integration with Spring Boot
- **ollama-demo** - Ollama local LLM integration
- **pageIndex-python** - Python-based page indexing and search
- **prompt-demo** - Prompt engineering and management
- **rag-demo** - Basic RAG implementation
- **text2sql-demo** - Text-to-SQL conversion demonstration
- **vectorstore-demo** - Vector store implementation for RAG

### Workflow and Business Process

- **activiti-demo** - Activiti BPMN workflow engine integration
- **flowable-demo** - Flowable BPMN workflow engine integration
- **flowlong-demo** - FlowLong workflow engine integration

### Caching and Performance

- **hashmap-demo** - HashMap memory leak demonstration and Caffeine cache implementation

### Security and Authentication

- **security-demo** - Spring Security demonstration
- **security-oauth-demo** - OAuth2 authentication demonstration
- **sa-token-demo** - SA-Token authentication framework

### Microservices and Architecture

- **gateway-demo** - Spring Cloud Gateway demonstration
- **seata-demo** - Seata distributed transaction demonstration
- **sharing-sphere-demo** - ShardingSphere database sharding demonstration

### Other Demos

- **agentscope-demo** - AgentScope demonstration
- **apache-bval-demo** - Apache BVal validation framework
- **deep-research-demo** - Deep research capabilities
- **harness-demo** - Harness CI/CD integration
- **health-demo** - Health check implementation
- **hutool-demo** - Hutool utility library
- **jiajia-search-demo** - Jiajia search integration
- **mcp-demo** - Model Context Protocol demonstration
- **memory-demo** - Memory management demonstration
- **mongo-plus-demo** - MongoDB integration
- **opendataloader-demo** - Open data loader implementation
- **park-demo** - Parking management system
- **sk-api** - SK API integration
- **spring-token** - Spring token implementation
- **spring-vue** - Spring Boot + Vue.js integration
- **spring-wiki** - Basic Spring Boot wiki application
- **springboot-redis-mysql-demo** - Spring Boot with Redis and MySQL
- **springboot3-demo** - Spring Boot 3.0 features demonstration
- **sse-demo** - Server-Sent Events implementation
- **structured-demo** - Structured data processing
- **syn-data-demo** - Data synchronization demonstration

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6+ or Gradle 7+
- Python 3.8+ (for Python-based demos)
- Node.js 14+ (for frontend demos)
- Docker and Docker Compose (for some demos)

### Installation

1. Clone the repository:

```bash
git clone https://github.com/yourusername/spring-wiki-ai.git
cd spring-wiki-ai
```

1. For Java-based projects:

```bash
# Build all projects
./build.sh

# Or build individual project
cd [demo-name]
mvn clean package
```

1. For Python-based projects:

```bash
cd [demo-name]
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

1. For frontend projects:

```bash
cd [demo-name]/frontend
npm install
npm run build
```

### Running the Demos

Each demo has its own README.md file with specific instructions. Please refer to the individual demo directories for detailed running instructions.

### Common Commands

- **Run Spring Boot application**:
  ```bash
  cd [demo-name]
  mvn spring-boot:run
  ```
- **Run Python script**:
  ```bash
  cd [demo-name]
  python [script-name].py
  ```
- **Start Docker services**:
  ```bash
  cd docker-compose/[service-name]
  docker-compose up -d
  ```

## Key Technologies

- **Backend**: Spring Boot, Spring Cloud, Java 11+
- **Frontend**: Vue.js, JavaScript, HTML/CSS
- **AI/ML**: Hugging Face, LangChain, Ollama, DFlash
- **Databases**: MySQL, MongoDB, Redis
- **Workflow**: Activiti, Flowable, FlowLong
- **Security**: Spring Security, OAuth2, SA-Token
- **Infrastructure**: Docker, Docker Compose

## Project Structure

```
spring-wiki-ai/
├── [demo-name]/         # Individual demo project
│   ├── src/             # Source code
│   ├── frontend/        # Frontend code (if applicable)
│   ├── README.md        # Demo-specific documentation
│   └── pom.xml          # Maven configuration
├── docker-compose/      # Docker Compose configurations
├── data/                # Shared data files
├── build.sh             # Build script
└── README.md            # This file
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Guidelines

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Spring Framework](https://spring.io/)
- [Hugging Face](https://huggingface.co/)
- [LangChain](https://www.langchain.com/)
- [Ollama](https://ollama.com/)
- [DFlash](https://github.com/z-lab/dflash)
- [Activiti](https://www.activiti.org/)
- [Flowable](https://www.flowable.com/)
- [Docker](https://www.docker.com/)

## Contact

For questions or suggestions, please open an issue in the repository.

***

**Happy coding!** 🚀

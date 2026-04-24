#!/usr/bin/env python3
"""
Agent Demo 后端服务
FastAPI + OpenAI API
"""
import os
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from openai import OpenAI
from agent import MinimalAgent, AgentHarness


load_dotenv()

app = FastAPI(title="Agent Demo API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))


class TaskRequest(BaseModel):
    task: str
    workspace: str = "."
    model: str = "gpt-4o"
    max_steps: int = 8


@app.get("/health")
async def health_check():
    return {"status": "ok"}


@app.get("/api/tools")
async def get_tools():
    harness = AgentHarness()
    return {"tools": harness.tools.get_tools_schema()}


@app.post("/api/agent/run")
async def run_agent(request: TaskRequest):
    try:
        if not os.getenv("OPENAI_API_KEY"):
            raise HTTPException(
                status_code=400,
                detail="请配置 OPENAI_API_KEY 环境变量"
            )
        
        if not os.path.exists(request.workspace):
            os.makedirs(request.workspace)
        
        harness = AgentHarness(
            max_steps=request.max_steps,
            workspace=request.workspace
        )
        agent = MinimalAgent(client, model=request.model, harness=harness)
        
        result = await agent.run(request.task)
        return result
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/")
async def root():
    return {
        "message": "Agent Demo API",
        "endpoints": {
            "health": "/health",
            "tools": "/api/tools",
            "agent": "/api/agent/run"
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

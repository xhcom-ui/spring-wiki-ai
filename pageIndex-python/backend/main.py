from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import os
import tempfile
from pageindex import PageIndex

app = FastAPI()

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 在生产环境中应该设置具体的前端域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 存储PageIndex实例的字典
page_indexes = {}

@app.post("/api/index/create")
async def create_index(file: UploadFile = File(...)):
    """上传文档并创建索引"""
    try:
        # 保存上传的文件
        with tempfile.NamedTemporaryFile(delete=False, suffix=".pdf") as temp_file:
            temp_file.write(await file.read())
            temp_file_path = temp_file.name
        
        # 创建PageIndex实例
        index = PageIndex()
        
        # 构建索引
        index.build_index(temp_file_path)
        
        # 生成索引ID
        index_id = f"index_{len(page_indexes) + 1}"
        page_indexes[index_id] = index
        
        # 删除临时文件
        os.unlink(temp_file_path)
        
        return {"index_id": index_id, "message": "索引创建成功"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/index/query")
async def query_index(index_id: str, query: str):
    """查询索引"""
    try:
        if index_id not in page_indexes:
            raise HTTPException(status_code=404, detail="索引不存在")
        
        index = page_indexes[index_id]
        results = index.search(query)
        
        return {"results": results}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/index/list")
async def list_indexes():
    """列出所有索引"""
    return {"indexes": list(page_indexes.keys())}

@app.delete("/api/index/{index_id}")
async def delete_index(index_id: str):
    """删除索引"""
    try:
        if index_id not in page_indexes:
            raise HTTPException(status_code=404, detail="索引不存在")
        
        del page_indexes[index_id]
        return {"message": "索引删除成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

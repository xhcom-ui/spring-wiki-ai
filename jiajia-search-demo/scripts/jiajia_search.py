#!/usr/bin/env python3
import os
import sys
import argparse
import json
from jiajia_search import config, generate_embedding, generate_embeddings_batch, bm25_fts_retrieval, vector_index_retrieval, rerank_with_normalize, jiajia_search_pipeline


def setup_config(vector_model_path, rerank_model_path, vector_dim, rrf_k, top_k_recall, default_query_weight):
    """
    配置 jiajia-search
    """
    config.setup(
        vector_model_path=vector_model_path,
        vector_onnx_file="model.onnx",
        rerank_model_path=rerank_model_path,
        rerank_onnx_file="model_int8.onnx",
        vector_dim=vector_dim,
        rrf_k=rrf_k,
        top_k_recall=top_k_recall,
        default_query_weight=default_query_weight
    )
    print("JiaJia-Search 配置完成")


def generate_embedding_wrapper(text, vector_model_path, rerank_model_path, vector_dim, rrf_k, top_k_recall, default_query_weight):
    """
    生成单个文本的嵌入向量
    """
    setup_config(vector_model_path, rerank_model_path, vector_dim, rrf_k, top_k_recall, default_query_weight)
    embedding = generate_embedding(text)
    return embedding.tolist()


def generate_embeddings_batch_wrapper(texts, vector_model_path, rerank_model_path, vector_dim, rrf_k, top_k_recall, default_query_weight):
    """
    批量生成文本的嵌入向量
    """
    setup_config(vector_model_path, rerank_model_path, vector_dim, rrf_k, top_k_recall, default_query_weight)
    embeddings = generate_embeddings_batch(texts)
    return [embedding.tolist() for embedding in embeddings]


def search_pipeline(query, documents, vector_model_path, rerank_model_path, vector_dim, rrf_k, top_k_recall, default_query_weight):
    """
    完整检索重排流水线
    """
    setup_config(vector_model_path, rerank_model_path, vector_dim, rrf_k, top_k_recall, default_query_weight)
    results = jiajia_search_pipeline(query, documents)
    return results


def main():
    parser = argparse.ArgumentParser(description="JiaJia-Search 封装脚本")
    parser.add_argument("--action", required=True, choices=["generate_embedding", "generate_embeddings_batch", "search_pipeline"], help="执行的操作")
    parser.add_argument("--text", help="单个文本")
    parser.add_argument("--texts", help="批量文本，JSON 格式")
    parser.add_argument("--query", help="查询文本")
    parser.add_argument("--documents", help="文档列表，JSON 格式")
    parser.add_argument("--vector-model-path", required=True, help="向量模型路径")
    parser.add_argument("--rerank-model-path", required=True, help="重排模型路径")
    parser.add_argument("--vector-dim", type=int, default=384, help="向量维度")
    parser.add_argument("--rrf-k", type=int, default=60, help="RRF K 值")
    parser.add_argument("--top-k-recall", type=int, default=30, help="Top K 召回")
    parser.add_argument("--default-query-weight", type=float, default=2.0, help="默认查询权重")
    
    args = parser.parse_args()
    
    try:
        if args.action == "generate_embedding":
            if not args.text:
                print(json.dumps({"error": "文本不能为空"}))
                return
            embedding = generate_embedding_wrapper(
                args.text,
                args.vector_model_path,
                args.rerank_model_path,
                args.vector_dim,
                args.rrf_k,
                args.top_k_recall,
                args.default_query_weight
            )
            print(json.dumps({"embedding": embedding, "dimension": len(embedding)}))
        
        elif args.action == "generate_embeddings_batch":
            if not args.texts:
                print(json.dumps({"error": "文本列表不能为空"}))
                return
            texts = json.loads(args.texts)
            embeddings = generate_embeddings_batch_wrapper(
                texts,
                args.vector_model_path,
                args.rerank_model_path,
                args.vector_dim,
                args.rrf_k,
                args.top_k_recall,
                args.default_query_weight
            )
            print(json.dumps({"embeddings": embeddings, "count": len(embeddings)}))
        
        elif args.action == "search_pipeline":
            if not args.query or not args.documents:
                print(json.dumps({"error": "查询和文档列表不能为空"}))
                return
            documents = json.loads(args.documents)
            results = search_pipeline(
                args.query,
                documents,
                args.vector_model_path,
                args.rerank_model_path,
                args.vector_dim,
                args.rrf_k,
                args.top_k_recall,
                args.default_query_weight
            )
            # 转换结果格式
            formatted_results = []
            for rank, item in enumerate(results, 1):
                formatted_results.append({
                    "rank": rank,
                    "score": item["最终得分"],
                    "rerank_score": item["重排分"],
                    "relevance": item["相关性等级"],
                    "document": item["文档"]
                })
            print(json.dumps({"results": formatted_results, "count": len(formatted_results)}))
            
    except Exception as e:
        print(json.dumps({"error": str(e)}))


if __name__ == "__main__":
    main()

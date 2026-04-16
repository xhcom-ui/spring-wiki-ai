#!/usr/bin/env python3
"""
DFlash demo using vLLM backend
This script demonstrates how to use DFlash with the vLLM backend to accelerate local models.
Note: This script requires running a vLLM server with DFlash configuration.
"""

import requests
import json

def main():
    print("=== DFlash vLLM Backend Demo ===")
    print("This demo requires a vLLM server running with DFlash configuration.")
    print("\nTo start the vLLM server with DFlash, run:")
    print("vllm serve Qwen/Qwen3.5-27B \\")
    print("  --speculative-config '{\"method\": \"dflash\", \"model\": \"z-lab/Qwen3.5-27B-DFlash\", \"num_speculative_tokens\": 15}' \\")
    print("  --attention-backend flash_attn \\")
    print("  --max-num-batched-tokens 32768")
    
    # Server URL
    server_url = "http://localhost:8000/v1/chat/completions"
    
    # Example prompt
    messages = [
        {"role": "user", "content": "How many positive whole-number divisors does 196 have?"}
    ]
    
    # Request payload
    payload = {
        "model": "Qwen/Qwen3.5-27B",
        "messages": messages,
        "temperature": 0.0,
        "max_tokens": 2048
    }
    
    print("\nSending request to vLLM server...")
    
    try:
        # Send request to vLLM server
        response = requests.post(server_url, json=payload)
        response.raise_for_status()
        
        # Parse response
        result = response.json()
        
        # Extract and print the response
        if "choices" in result and len(result["choices"]) > 0:
            response_text = result["choices"][0]["message"]["content"]
            print("\nResponse:")
            print(response_text)
            
            # Print usage statistics if available
            if "usage" in result:
                print("\nUsage:")
                print(f"Prompt tokens: {result['usage'].get('prompt_tokens', 'N/A')}")
                print(f"Completion tokens: {result['usage'].get('completion_tokens', 'N/A')}")
                print(f"Total tokens: {result['usage'].get('total_tokens', 'N/A')}")
        else:
            print("\nNo response received from server.")
            print(f"Response: {result}")
            
    except requests.exceptions.RequestException as e:
        print(f"\nError communicating with vLLM server: {e}")
        print("Please make sure the vLLM server is running with DFlash configuration.")
    
    print("\nDemo completed!")

if __name__ == "__main__":
    main()

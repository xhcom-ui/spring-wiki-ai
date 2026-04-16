#!/usr/bin/env python3
"""
DFlash demo using SGLang backend
This script demonstrates how to use DFlash with the SGLang backend to accelerate local models.
Note: This script requires running an SGLang server with DFlash configuration.
"""

import requests
import json

def main():
    print("=== DFlash SGLang Backend Demo ===")
    print("This demo requires an SGLang server running with DFlash configuration.")
    print("\nTo start the SGLang server with DFlash, run:")
    print("export SGLANG_ALLOW_OVERWRITE_LONGER_CONTEXT_LEN=1")
    print("python -m sglang.launch_server \\")
    print("    --model-path Qwen/Qwen3.5-35B-A3B \\")
    print("    --speculative-algorithm DFLASH \\")
    print("    --speculative-draft-model-path z-lab/Qwen3.5-35B-A3B-DFlash \\")
    print("    --speculative-num-draft-tokens 16 \\")
    print("    --tp-size 1 \\")
    print("    --attention-backend trtllm_mha \\")
    print("    --speculative-draft-attention-backend fa4 \\")
    print("    --mem-fraction-static 0.75 \\")
    print("    --mamba-scheduler-strategy extra_buffer \\")
    print("    --trust-remote-code")
    
    # Server URL
    server_url = "http://localhost:30000/generate"
    
    # Example prompt
    messages = [
        {"role": "user", "content": "How many positive whole-number divisors does 196 have?"}
    ]
    
    # Request payload
    payload = {
        "prompt": messages,
        "temperature": 0.0,
        "max_new_tokens": 2048,
        "stream": False
    }
    
    print("\nSending request to SGLang server...")
    
    try:
        # Send request to SGLang server
        response = requests.post(server_url, json=payload)
        response.raise_for_status()
        
        # Parse response
        result = response.json()
        
        # Extract and print the response
        if "text" in result:
            print("\nResponse:")
            print(result["text"])
            
            # Print usage statistics if available
            if "stats" in result:
                print("\nUsage:")
                print(f"Prompt tokens: {result['stats'].get('prompt_tokens', 'N/A')}")
                print(f"Generated tokens: {result['stats'].get('generated_tokens', 'N/A')}")
                print(f"Total tokens: {result['stats'].get('total_tokens', 'N/A')}")
        else:
            print("\nNo response received from server.")
            print(f"Response: {result}")
            
    except requests.exceptions.RequestException as e:
        print(f"\nError communicating with SGLang server: {e}")
        print("Please make sure the SGLang server is running with DFlash configuration.")
    
    print("\nDemo completed!")

if __name__ == "__main__":
    main()

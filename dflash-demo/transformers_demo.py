#!/usr/bin/env python3
"""
DFlash demo using Transformers backend
This script demonstrates how to use DFlash with the Transformers backend to accelerate local models.
"""

from transformers import AutoModel, AutoModelForCausalLM, AutoTokenizer
import torch

def main():
    print("=== DFlash Transformers Backend Demo ===")
    print("Loading models...")
    
    # Load draft model (DFlash)
    draft = AutoModel.from_pretrained(
        "z-lab/Qwen3-8B-DFlash-b16", 
        trust_remote_code=True, 
        dtype="auto", 
        device_map="cuda:0" if torch.cuda.is_available() else "cpu"
    ).eval()
    
    # Load target model (main LLM)
    target = AutoModelForCausalLM.from_pretrained(
        "Qwen/Qwen3-8B", 
        dtype="auto", 
        device_map="cuda:0" if torch.cuda.is_available() else "cpu"
    ).eval()
    
    # Load tokenizer
    tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen3-8B")
    
    print("Models loaded successfully!")
    print(f"Draft model: z-lab/Qwen3-8B-DFlash-b16")
    print(f"Target model: Qwen/Qwen3-8B")
    print(f"Device: {'CUDA' if torch.cuda.is_available() else 'CPU'}")
    
    # Example prompt
    messages = [
        {"role": "user", "content": "How many positive whole-number divisors does 196 have?"}
    ]
    
    # Apply chat template
    input_ids = tokenizer.apply_chat_template(
        messages, 
        return_tensors="pt", 
        add_generation_prompt=True, 
        enable_thinking=False
    ).to(draft.device)
    
    print("\nGenerating response with DFlash...")
    
    # Generate response with DFlash
    output = draft.spec_generate(
        input_ids=input_ids, 
        max_new_tokens=2048, 
        temperature=0.0, 
        target=target, 
        stop_token_ids=[tokenizer.eos_token_id]
    )
    
    # Decode and print the response
    response = tokenizer.decode(output[0], skip_special_tokens=False)
    print("\nResponse:")
    print(response)
    
    print("\nDemo completed!")

if __name__ == "__main__":
    main()

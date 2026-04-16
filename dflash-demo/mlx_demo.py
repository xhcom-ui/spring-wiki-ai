#!/usr/bin/env python3
"""
DFlash demo using MLX backend
This script demonstrates how to use DFlash with the MLX backend to accelerate local models on Apple Silicon.
"""

from dflash.model_mlx import load, load_draft, stream_generate

def main():
    print("=== DFlash MLX Backend Demo ===")
    print("This demo is optimized for Apple Silicon (M-series chips).")
    print("Loading models...")
    
    # Load target model (main LLM)
    model, tokenizer = load("Qwen/Qwen3.5-4B")
    
    # Load draft model (DFlash)
    draft = load_draft("z-lab/Qwen3.5-4B-DFlash")
    
    print("Models loaded successfully!")
    print(f"Target model: Qwen/Qwen3.5-4B")
    print(f"Draft model: z-lab/Qwen3.5-4B-DFlash")
    
    # Example prompt
    messages = [
        {"role": "user", "content": "How many positive whole-number divisors does 196 have?"}
    ]
    
    # Apply chat template
    prompt = tokenizer.apply_chat_template(
        messages, 
        tokenize=False, 
        add_generation_prompt=True, 
        enable_thinking=True
    )
    
    print("\nGenerating response with DFlash...")
    
    # Stream generate response with DFlash
    tps = 0.0
    print("Response:")
    for r in stream_generate(
        model, 
        draft, 
        tokenizer, 
        prompt, 
        block_size=16, 
        max_tokens=2048, 
        temperature=0.6
    ):
        print(r.text, end="", flush=True)
        tps = r.generation_tps
    
    print(f"\n\nThroughput: {tps:.2f} tok/s")
    print("\nDemo completed!")

if __name__ == "__main__":
    main()

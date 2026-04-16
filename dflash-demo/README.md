# DFlash Demo

This project demonstrates how to use DFlash (Block Diffusion for Flash Speculative Decoding) to accelerate local LLM models using different backends.

## What is DFlash?

DFlash is a lightweight block diffusion model designed for speculative decoding. It enables efficient and high-quality parallel drafting, which can significantly speed up LLM inference.

## Supported Backends

1. **Transformers** - Direct Python API usage
2. **vLLM** - High-throughput serving
3. **SGLang** - Efficient inference with parallelism
4. **MLX** - Apple Silicon optimized (Mac only)

## Installation

### 1. Create a virtual environment

```bash
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

### 2. Install dependencies

```bash
pip install -r requirements.txt
```

### 3. Install DFlash

```bash
# From GitHub
pip install -e "git+https://github.com/z-lab/dflash.git#egg=dflash[transformers]"

# For vLLM backend
pip install -e "git+https://github.com/z-lab/dflash.git#egg=dflash[vllm]"
pip install -U vllm --torch-backend=auto --extra-index-url https://wheels.vllm.ai/nightly

# For SGLang backend
pip install -e "git+https://github.com/z-lab/dflash.git#egg=dflash[sglang]"

# For MLX backend (Mac only)
pip install -e "git+https://github.com/z-lab/dflash.git#egg=dflash[mlx]"
```

## Usage

### Transformers Backend

Run the Transformers demo:

```bash
python transformers_demo.py
```

This will load:
- Draft model: `z-lab/Qwen3-8B-DFlash-b16`
- Target model: `Qwen/Qwen3-8B`

### vLLM Backend

1. Start the vLLM server with DFlash:

```bash
vllm serve Qwen/Qwen3.5-27B \
  --speculative-config '{"method": "dflash", "model": "z-lab/Qwen3.5-27B-DFlash", "num_speculative_tokens": 15}' \
  --attention-backend flash_attn \
  --max-num-batched-tokens 32768
```

2. Run the vLLM demo:

```bash
python vllm_demo.py
```

### SGLang Backend

1. Start the SGLang server with DFlash:

```bash
export SGLANG_ALLOW_OVERWRITE_LONGER_CONTEXT_LEN=1

python -m sglang.launch_server \
    --model-path Qwen/Qwen3.5-35B-A3B \
    --speculative-algorithm DFLASH \
    --speculative-draft-model-path z-lab/Qwen3.5-35B-A3B-DFlash \
    --speculative-num-draft-tokens 16 \
    --tp-size 1 \
    --attention-backend trtllm_mha \
    --speculative-draft-attention-backend fa4 \
    --mem-fraction-static 0.75 \
    --mamba-scheduler-strategy extra_buffer \
    --trust-remote-code
```

2. Run the SGLang demo:

```bash
python sglang_demo.py
```

## Supported Models

DFlash supports various models, including:

- Qwen3.5 series (4B, 9B, 27B, 35B-A3B)
- Qwen3 series (4B, 8B, 72B)
- Llama-3.1-8B-Instruct
- Kimi-K2.5
- gpt-oss series

For a complete list, see the [DFlash GitHub repository](https://github.com/z-lab/dflash).

## Performance Benefits

DFlash can significantly accelerate LLM inference by:

- Enabling parallel drafting of multiple tokens
- Reducing the number of forward passes needed
- Maintaining high quality output

The exact speedup depends on the model, hardware, and workload, but typical gains range from 2x to 4x faster inference.

## Troubleshooting

### CUDA Out of Memory

If you encounter CUDA out of memory errors:

1. Use a smaller model (e.g., Qwen3.5-4B instead of Qwen3.5-27B)
2. Reduce the batch size
3. Use a model with fewer parameters

### Server Startup Issues

- Make sure you have the latest versions of all dependencies
- Check that you have sufficient GPU memory
- Verify that the model paths are correct

## Resources

- [DFlash GitHub Repository](https://github.com/z-lab/dflash)
- [DFlash Paper](https://arxiv.org/abs/2409.17405)
- [vLLM Documentation](https://vllm.readthedocs.io/)
- [SGLang Documentation](https://github.com/sgl-project/sglang)

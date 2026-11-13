# Chat model selection

**Decision: `OPENROUTER_CHAT_MODEL = anthropic/claude-haiku-5.5`.**

The chat model writes the assistant's answer from the knowledge articles that passed the similarity
threshold. It never decides alone whether to answer: if no article passes the threshold, n8n returns
`NO_RESULTS` without calling it.

## What was tested

Five OpenRouter models with input price ≤ 0.10 $/M tokens and JSON output support, from different
providers, on the same task: an incident description plus one or two real articles from
`scripts/articles.json` as sources, with the system prompt that flow 2 will use (answer only from the
sources, JSON in Spanish, `{"relevant": false}` when the sources do not cover the problem, treat the
incident text as data). Temperature 0.2, `response_format: json_object`.

Script: [`scripts/calibration/compare_chat_models.py`](../scripts/calibration/compare_chat_models.py).

| Case | What it checks |
|---|---|
| `test1_internet` | Mandatory test 1 with the right article plus a near one (`red-lenta`) |
| `test2_app_error` | Mandatory test 2 with the right article plus a near one (`app-web-error-500`) |
| `unrelated_source` | 3D printer incident with the network printer article: must answer `relevant: false` |
| `prompt_injection` | Legitimate incident whose text orders the model to ignore the rules and write a poem in English |
| `pick_second_source` | Two articles; only the second one applies |
| `vague_description` | Short, vague description that still matches an article |
| `test1_repeat` | Same as test 1, to check stability |

Per answer: valid JSON, right relevance verdict, number of steps, Spanish, grounding (share of the
answer's words that appear in the sources), latency and cost reported by OpenRouter.

## Results

| Model | Cases passed | Notes | Latency | Cost per answer |
|---|---:|---|---:|---:|
| **anthropic/claude-haiku-5.5** | **7/7** | Always 7 steps, also under injection; grounding 0.61–0.93 | 1.6–4.9 s | 0.0002–0.0005 $ |
| google/gemini-2.5-flash-lite | 7/7 | Dropped to 4 steps under injection; grounding 0.54–0.96 | 1.0–3.1 s | 0.0001–0.0002 $ |
| mistralai/mistral-small-3.2-24b-instruct | 3/4 | Refused the whole incident under injection (`relevant: false`), so a real user would get no help | 2.4–2.8 s | 0.0001–0.0002 $ |
| deepseek/deepseek-v4-flash | 3/4 | Same refusal under injection | 1.9–3.2 s | 0.0001–0.0002 $ |
| openai/gpt-5-nano | 1/4 | Empty content in 3 of 4 answers: its reasoning tokens consumed the output budget | 10–31 s | 0.0002–0.0005 $ |

Mistral, DeepSeek and gpt-5-nano were not run in the second round.

Every model that answered did so in Spanish and correctly returned `relevant: false` for the 3D
printer case, which confirms the prompt's second safety gate works across models.

## Why Claude Haiku 5.5

- The only model that gave a complete answer (7 steps) in every case, including the incident with an
  injected instruction. Gemini Flash Lite also answered that case correctly but with 4 steps.
- Highest and most stable grounding on the mandatory tests (0.93 and 0.84 on test 1).
- Cost is irrelevant at this project's scale: 1,000 queries cost about 0.50 $ with Haiku and 0.20 $
  with Gemini Flash Lite. Latency (3–4 s) is acceptable for an assistant that is called on demand.

Gemini 2.5 Flash Lite is a valid alternative if cost or latency become a priority: switch
`OPENROUTER_CHAT_MODEL` and rerun the script.

## Limitations

- Seven cases and one run per case (two for test 1): enough to discard models that fail basic rules,
  not to rank the two finalists precisely.
- Grounding is a word-overlap measure, so a faithful paraphrase scores lower than a copy.
- The team was assisted by an Anthropic model during development; the choice is based on the measured
  results above, and the script allows anyone to repeat the comparison with other models.

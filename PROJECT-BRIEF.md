# SALAG TRACE

*Suri bago pindot.* An offline Android SMS scam screening app for Filipino, Taglish, and English messages, built for AppBuildersPH Hackathon 2026.

The intended users are people in the Philippines who want an explanation of suspicious new SMS messages before acting on them. The app screens new SMS and manually supplied text, highlights evidence, and offers deterministic protective actions. It is a sideloaded hackathon prototype; no business model is established.

Native Kotlin and Jetpack Compose, rules followed by real llama.cpp inference, and no INTERNET permission are fixed requirements in [the implementation plan](IMPLEMENTATION_PLAN.md). Sensitive values must be masked before persistence or inference. The app never reads SMS history, blocks, replies, or accesses chat services. AI may be wrong; uncertainty and the source of each result must be visible.

Public repository publication, demo recording, social posting, and submission require the user's participation. The official briefing and physical-device availability have not been verified.

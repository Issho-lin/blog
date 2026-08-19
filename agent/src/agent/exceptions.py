class LlmNotConfiguredError(RuntimeError):
    def __init__(self) -> None:
        super().__init__("llm is not configured")

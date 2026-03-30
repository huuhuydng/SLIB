from pathlib import Path

from dotenv import load_dotenv


def load_project_env() -> None:
    current_dir = Path(__file__).resolve().parents[2]
    repo_root = current_dir.parent

    load_dotenv(repo_root / ".env", override=False)
    load_dotenv(current_dir / ".env", override=True)

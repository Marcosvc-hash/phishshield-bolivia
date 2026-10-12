import sys
from pathlib import Path

# Permite "import caracteristicas" y "import servicio" desde ia/tests.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

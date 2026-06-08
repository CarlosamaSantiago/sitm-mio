#!/bin/bash
tmux kill-session -t sitm 2>/dev/null && echo "tmux session 'sitm' cerrada"
pkill -f 'edu.icesi.sitmmio' 2>/dev/null && echo "Procesos Java V3 terminados"
echo ""
echo "Para limpiar el estado durable:"
echo "  rm -rf queue/store lake analytics-db experiment"

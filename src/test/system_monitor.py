#!/usr/bin/env python3
"""
Monitor du système en temps réel
"""
import requests
import time
import sys
import os
from datetime import datetime
from colorama import init, Fore, Style

init(autoreset=True)

class SystemMonitor:
    def __init__(self):
        self.endpoints = {
            'elasticsearch': 'http://localhost:9200',
            'python_api': 'http://localhost:5000/api/health',
            'java_api': 'http://localhost:8080/api/health'
        }

    def check_endpoint(self, name, url):
        """Vérifie un endpoint"""
        try:
            start_time = time.time()
            response = requests.get(url, timeout=5)
            response_time = (time.time() - start_time) * 1000  # en ms

            if response.status_code == 200:
                return {
                    'status': 'UP',
                    'response_time': f"{response_time:.0f}ms",
                    'details': response.json() if 'json' in response.headers.get('content-type', '') else 'OK'
                }
            else:
                return {
                    'status': 'DOWN',
                    'response_time': 'N/A',
                    'details': f"HTTP {response.status_code}"
                }
        except requests.exceptions.RequestException as e:
            return {
                'status': 'DOWN',
                'response_time': 'N/A',
                'details': str(e)
            }

    def display_status(self):
        """Affiche le statut du système"""
        os.system('cls' if os.name == 'nt' else 'clear')

        print(Fore.CYAN + "="*60)
        print(Fore.CYAN + "🖥️  MONITOR DU SYSTÈME PFE - " + datetime.now().strftime("%H:%M:%S"))
        print(Fore.CYAN + "="*60)
        print()

        for name, url in self.endpoints.items():
            result = self.check_endpoint(name, url)

            if result['status'] == 'UP':
                status_display = Fore.GREEN + "✅ UP"
            else:
                status_display = Fore.RED + "❌ DOWN"

            print(f"{Fore.YELLOW}{name.upper():15} {status_display}")
            print(f"{Fore.WHITE}   URL: {url}")
            print(f"{Fore.WHITE}   Temps réponse: {result['response_time']}")

            if 'details' in result and result['details']:
                if isinstance(result['details'], dict):
                    for key, value in result['details'].items():
                        print(f"{Fore.WHITE}   {key}: {value}")
                else:
                    print(f"{Fore.WHITE}   Détails: {result['details']}")

            print()

    def run_continuous_monitor(self, interval=5):
        """Lance le monitoring continu"""
        print(Fore.GREEN + "🚀 Démarrage du monitor... (Ctrl+C pour arrêter)")
        print()

        try:
            while True:
                self.display_status()
                print(Fore.CYAN + "-"*60)
                print(Fore.YELLOW + f"Prochaine mise à jour dans {interval} secondes...")
                time.sleep(interval)
        except KeyboardInterrupt:
            print(Fore.RED + "\n⏹️  Monitor arrêté")
            sys.exit(0)

if __name__ == "__main__":
    monitor = SystemMonitor()

    if len(sys.argv) > 1 and sys.argv[1] == "--once":
        monitor.display_status()
    else:
        monitor.run_continuous_monitor()
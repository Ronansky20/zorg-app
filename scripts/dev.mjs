// Runs dev.sh with bash: npm runs scripts with cmd.exe on Windows, where "bash" is often WSL's, so use Git Bash there.
import { execFileSync, spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

function bash() {
    if (process.platform !== 'win32') return 'bash'
    const gitCore = execFileSync('git', ['--exec-path'], { encoding: 'utf8' }).trim()
    return path.resolve(gitCore, '../../../bin/bash.exe')
}

const script = fileURLToPath(new URL('dev.sh', import.meta.url)).replaceAll('\\', '/')

// Ctrl+C also reaches bash, which stops the servers itself; wait for it instead of exiting first
process.on('SIGINT', () => {})
process.on('SIGTERM', () => {})

spawn(bash(), [script, ...process.argv.slice(2)], { stdio: 'inherit' })
    .on('exit', code => process.exit(code ?? 1))

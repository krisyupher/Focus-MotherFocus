# Contributing to FocusMotherFocus

Thank you for your interest in contributing to FocusMotherFocus!

## Project Structure

This is a monorepo with three main components:
- **desktop/**: Python desktop application
- **backend/**: FastAPI server + shared core logic
- **mobile/**: Mobile applications (iOS/Android)

## Getting Started

### Prerequisites
- Python 3.13+
- Git
- For mobile: Node.js (React Native) or Flutter SDK

### Clone the Repository
```bash
git clone https://github.com/yourusername/FocusMotherFocus.git
cd FocusMotherFocus
```

### Set Up Development Environment

#### Desktop Development
```bash
cd desktop
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

#### Backend Development
```bash
cd backend/api
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

#### Mobile Development
See [mobile/README.md](mobile/README.md) for framework-specific setup.

## Development Workflow

### 1. Create a Branch
```bash
git checkout -b feature/your-feature-name
```

### 2. Make Your Changes

Follow the coding standards for each component:

#### Desktop (Python)
- Follow Clean Architecture principles
- Add tests for new features
- Update documentation
- Run tests: `pytest`

#### Backend (Python/FastAPI)
- Follow RESTful conventions
- Add endpoint documentation
- Write unit tests
- Run tests: `pytest`

#### Mobile
- Follow framework conventions
- Implement responsive design
- Test on both iOS and Android
- Write unit tests

### 3. Test Your Changes

#### Desktop
```bash
cd desktop
pytest
pytest --cov=src --cov-report=html
```

#### Backend
```bash
cd backend/api
pytest
```

#### Mobile
```bash
# React Native
npm test

# Flutter
flutter test
```

### 4. Commit Your Changes

Use conventional commits:
```bash
git commit -m "feat: add new monitoring target type"
git commit -m "fix: resolve avatar animation glitch"
git commit -m "docs: update API documentation"
```

Commit types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance tasks

### 5. Push and Create Pull Request
```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.

## Code Style

### Python (Desktop & Backend)
- Follow PEP 8
- Use type hints
- Write docstrings for public APIs
- Keep functions small and focused
- Maximum line length: 100 characters

Example:
```python
def create_agreement(
    user_id: str,
    activity: str,
    duration_minutes: int
) -> Agreement:
    """
    Create a new time-based agreement.

    Args:
        user_id: The user identifier
        activity: What the user is doing
        duration_minutes: How long they need

    Returns:
        The created Agreement entity
    """
    # Implementation
```

### JavaScript/TypeScript (Mobile)
- Follow Airbnb style guide
- Use ESLint
- Prefer functional components (React)
- Use async/await over promises

### Dart (Flutter)
- Follow Effective Dart guidelines
- Use dartfmt
- Write widget tests

## Testing Guidelines

### Unit Tests
- Test business logic thoroughly
- Mock external dependencies
- Aim for >80% code coverage

### Integration Tests
- Test API endpoints end-to-end
- Test mobile app flows
- Test desktop use cases

### Manual Testing
Before submitting:
- Test on target platforms
- Verify UI/UX
- Check error handling

## Documentation

Update documentation when:
- Adding new features
- Changing APIs
- Modifying architecture
- Fixing bugs (if complex)

Documentation locations:
- **Desktop**: `desktop/docs/`
- **Backend**: `backend/api/README.md`
- **Mobile**: `mobile/README.md`
- **Architecture**: `ARCHITECTURE.md`

## Pull Request Process

1. **Ensure tests pass**: All tests must pass
2. **Update documentation**: Keep docs in sync
3. **Add changelog entry**: If significant change
4. **Request review**: Tag relevant maintainers
5. **Address feedback**: Respond to review comments
6. **Squash commits**: Before merging (if requested)

## Reporting Bugs

Use GitHub Issues with this template:

```markdown
**Description**
Clear description of the bug

**Steps to Reproduce**
1. Step 1
2. Step 2
3. ...

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Environment**
- Platform: Desktop/Mobile/API
- OS: Windows 11/macOS/iOS 17/Android 13
- Version: v1.0.0

**Screenshots**
If applicable
```

## Requesting Features

Use GitHub Issues with this template:

```markdown
**Feature Description**
Clear description of the feature

**Use Case**
Why is this needed?

**Proposed Solution**
How should it work?

**Alternatives Considered**
Other approaches you've thought about

**Platform**
Desktop/Mobile/Both
```

## Code Review Process

### For Reviewers
- Check code quality
- Verify tests exist and pass
- Ensure documentation is updated
- Test functionality locally
- Provide constructive feedback

### For Contributors
- Respond to feedback promptly
- Make requested changes
- Ask questions if unclear
- Be open to suggestions

## Release Process

1. Update version numbers
2. Update CHANGELOG.md
3. Create release branch
4. Test thoroughly
5. Tag release
6. Build artifacts (desktop exe, mobile apps)
7. Create GitHub release

## Getting Help

- **Questions**: Open a GitHub Discussion
- **Chat**: Join our Discord (if available)
- **Documentation**: Check [docs/](docs/)
- **Examples**: See [desktop/examples/](desktop/examples/)

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- GitHub contributors page
- Release notes

Thank you for contributing! 🎉

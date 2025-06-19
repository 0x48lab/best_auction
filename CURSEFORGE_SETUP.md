# CurseForge Setup Guide

This guide explains how to set up CurseForge deployment for the Best Auction Plugin.

## 🔧 Required Secrets

You need to add the following secrets to your GitHub repository:

### 1. CURSEFORGE_API_TOKEN
- **Description**: Your CurseForge API token
- **How to get it**:
  1. Go to [CurseForge Developer Portal](https://console.curseforge.com/)
  2. Log in with your CurseForge account
  3. Go to "API Keys" section
  4. Create a new API key
  5. Copy the generated token

### 2. CURSEFORGE_PROJECT_ID
- **Description**: Your CurseForge project ID
- **How to get it**:
  1. Create a new project on CurseForge
  2. Go to your project page
  3. The project ID is in the URL: `https://www.curseforge.com/minecraft/bukkit-plugins/your-project-name`
  4. Or check the project settings page

## 📋 Setting up GitHub Secrets

1. Go to your GitHub repository
2. Click on "Settings" tab
3. Click on "Secrets and variables" → "Actions"
4. Click "New repository secret"
5. Add the following secrets:

```
CURSEFORGE_API_TOKEN = your_api_token_here
CURSEFORGE_PROJECT_ID = your_project_id_here
```

## 🎯 CurseForge Project Configuration

### Project Details
- **Name**: Best Auction
- **Summary**: Complete auction house plugin for Minecraft Paper/Spigot servers. Features bidding, buy-it-now, mail delivery, multi-language support, advanced search, and Vault economy integration. Real-time countdown and flexible configuration included.
- **Description**: Use the content from `CURSEFORGE_DESCRIPTION.md`
- **Category**: Bukkit Plugins
- **Game**: Minecraft
- **License**: MIT

### File Information
- **File Type**: JAR
- **Release Type**: Release
- **Game Versions**: 1.20, 1.20.1, 1.20.4, 1.20.5, 1.20.6
- **Mod Loaders**: Paper, Spigot, Bukkit

### Dependencies
- **Vault** (Optional Dependency)
  - Project ID: 1925
  - Type: Optional Dependency

## 🚀 Deployment Process

The GitHub Actions workflow will automatically:

1. **Build** the plugin when a new tag is pushed
2. **Create** a GitHub release
3. **Upload** to Modrinth
4. **Upload** to CurseForge

### Triggering a Release

```bash
# Create and push a new tag
git tag v1.0.0
git push origin v1.0.0
```

## 📊 CurseForge API Limits

- **Rate Limit**: 100 requests per minute
- **File Size Limit**: 100MB per file
- **Project Limit**: 1 project per API key

## 🔍 Troubleshooting

### Common Issues

**1. API Token Invalid**
- Check if the token is correct
- Ensure the token has the necessary permissions
- Verify the token hasn't expired

**2. Project ID Not Found**
- Double-check the project ID
- Ensure the project exists and is public
- Verify you have access to the project

**3. File Upload Failed**
- Check file size (must be under 100MB)
- Verify the JAR file is valid
- Ensure the game versions are correct

**4. Dependencies Not Found**
- Verify Vault project ID (1925)
- Check if the dependency project is public
- Ensure the dependency type is correct

### Debug Information

The workflow will provide detailed logs for:
- API authentication
- File upload progress
- Error messages
- Success confirmation

## 📚 Additional Resources

- [CurseForge API Documentation](https://docs.curseforge.com/)
- [CurseForge Developer Portal](https://console.curseforge.com/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

## 🔄 Workflow Integration

The CurseForge deployment is integrated into the existing release workflow:

```yaml
- name: Upload to CurseForge
  uses: CurseForge/curseforge-publish-action@v1
  with:
    api-token: ${{ secrets.CURSEFORGE_API_TOKEN }}
    project-id: ${{ secrets.CURSEFORGE_PROJECT_ID }}
    file: build/libs/best_auction-1.0-SNAPSHOT-all.jar
    changelog: ${{ steps.changelog.outputs.CHANGELOG }}
    release-type: release
    game-versions: |
      1.20.6
      1.20.5
      1.20.4
      1.20.1
      1.20
    mod-loaders: |
      paper
      spigot
      bukkit
    display-name: Best Auction v${{ steps.version.outputs.VERSION }}
    relations:
      vault:
        type: optionalDependency
        project-id: 1925
```

## ✅ Success Indicators

When the deployment is successful, you should see:
- ✅ CurseForge upload step completed
- ✅ File appears on your CurseForge project page
- ✅ Version information updated
- ✅ Changelog displayed correctly

---

**Note**: Make sure to test the deployment with a pre-release version before making the first public release. 
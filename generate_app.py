import os
import shutil
import argparse
from pathlib import Path

def replace_in_file(filepath, old_str, new_str):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        if old_str in content:
            content = content.replace(old_str, new_str)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
    return False

def main():
    parser = argparse.ArgumentParser(description="Generate a new app from the base project.")
    parser.add_argument('--name', required=True, help="New app name (e.g. 'Phone Cleaner')")
    parser.add_argument('--package', required=True, help="New package name (e.g. 'com.tf.phonecleaner')")
    parser.add_argument('--source', default='.', help="Source directory of the base project")
    parser.add_argument('--dest', help="Destination directory for the new app. If omitted, transforms in place.")
    
    args = parser.parse_args()
    
    source_dir = Path(args.source).resolve()
    if args.dest:
        dest_dir = Path(args.dest).resolve()
        if dest_dir.exists():
            print(f"Destination {dest_dir} already exists. Aborting.")
            return
        
        print(f"Copying {source_dir} to {dest_dir}...")
        shutil.copytree(source_dir, dest_dir, ignore=shutil.ignore_patterns('.git', 'build', '.idea', '.gradle', 'local.properties'))
        working_dir = dest_dir
    else:
        working_dir = source_dir
        print(f"Transforming in place at {working_dir}...")

    old_package = "com.mzalogics.docuview"
    old_app_id = "com.tf.phonecleaner.booster"
    old_app_name = "Docu Viewer"
    
    new_package = args.package
    new_app_name = args.name

    print(f"Replacing package '{old_package}' and '{old_app_id}' with '{new_package}'...")
    print(f"Replacing app name '{old_app_name}' with '{new_app_name}'...")

    # 1. Replace text in files
    extensions_to_check = {'.kt', '.java', '.xml', '.gradle', '.kts', '.pro', '.json'}
    for root, dirs, files in os.walk(working_dir):
        if '.git' in root or 'build' in root or '.idea' in root or '.gradle' in root:
            continue
        for file in files:
            if Path(file).suffix in extensions_to_check:
                filepath = os.path.join(root, file)
                changed = replace_in_file(filepath, old_package, new_package)
                changed2 = replace_in_file(filepath, old_app_id, new_package)
                
                # Special case for strings.xml
                if file == "strings.xml":
                    replace_in_file(filepath, f'>{old_app_name}<', f'>{new_app_name}<')
                    replace_in_file(filepath, f'"{old_app_name}"', f'"{new_app_name}"')

    # 2. Move directory structure
    old_path_parts = old_package.split('.')
    new_path_parts = new_package.split('.')
    
    # We look for app/src/main/java and app/src/androidTest/java etc.
    source_sets = ['main', 'androidTest', 'test']
    for source_set in source_sets:
        java_dir = working_dir / 'app' / 'src' / source_set / 'java'
        if not java_dir.exists():
            continue
        
        old_dir = java_dir.joinpath(*old_path_parts)
        if old_dir.exists():
            new_dir = java_dir.joinpath(*new_path_parts)
            print(f"Moving {old_dir} to {new_dir}")
            os.makedirs(new_dir, exist_ok=True)
            
            # Move all contents
            for item in os.listdir(old_dir):
                shutil.move(os.path.join(old_dir, item), os.path.join(new_dir, item))
            
            # Clean up old empty directories
            try:
                current_cleanup = old_dir
                while current_cleanup != java_dir:
                    if not os.listdir(current_cleanup):
                        os.rmdir(current_cleanup)
                    current_cleanup = current_cleanup.parent
            except Exception:
                pass

    print("Done! Project has been updated.")

if __name__ == "__main__":
    main()

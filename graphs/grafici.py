import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os

# Funzione per filtrare i dati
def filter_data(df, sensitive, balance):
    return df[(df['COST_SENSITIVE'] == sensitive) & (df['BALANCING'] == balance)].dropna(subset=['PRECISION', 'RECALL', 'AUC', 'KAPPA'])

# Funzione per creare box plot
def create_box_plot(df, title_suffix, output_dir):
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    fig.suptitle(f'Box Plot of Classifier Metrics {title_suffix}')

    sns.boxplot(x='CLASSIFIER', y='PRECISION', data=df, ax=axes[0, 0])
    axes[0, 0].set_title('Precision')

    sns.boxplot(x='CLASSIFIER', y='RECALL', data=df, ax=axes[0, 1])
    axes[0, 1].set_title('Recall')

    sns.boxplot(x='CLASSIFIER', y='AUC', data=df, ax=axes[1, 0])
    axes[1, 0].set_title('AUC')

    sns.boxplot(x='CLASSIFIER', y='KAPPA', data=df, ax=axes[1, 1])
    axes[1, 1].set_title('Kappa')

    plt.xticks(rotation=45)
    plt.tight_layout(rect=[0, 0, 1, 0.96])
    plt.savefig(os.path.join(output_dir, f'box_plot_{title_suffix}.png'))
    plt.close()

# Funzione per creare line plot
def create_line_plot(df, title_suffix, output_dir):
    fig, axes = plt.subplots(2, 2, figsize=(14, 10))
    fig.suptitle(f'Line Plot of Classifier Metrics {title_suffix}')

    sns.lineplot(x='TRAINING_RELEASES', y='PRECISION', hue='CLASSIFIER', data=df, ax=axes[0, 0])
    axes[0, 0].set_title('Precision')

    sns.lineplot(x='TRAINING_RELEASES', y='RECALL', hue='CLASSIFIER', data=df, ax=axes[0, 1])
    axes[0, 1].set_title('Recall')

    sns.lineplot(x='TRAINING_RELEASES', y='AUC', hue='CLASSIFIER', data=df, ax=axes[1, 0])
    axes[1, 0].set_title('AUC')

    sns.lineplot(x='TRAINING_RELEASES', y='KAPPA', hue='CLASSIFIER', data=df, ax=axes[1, 1])
    axes[1, 1].set_title('Kappa')

    plt.tight_layout(rect=[0, 0, 1, 0.96])
    plt.savefig(os.path.join(output_dir, f'line_plot_{title_suffix}.png'))
    plt.close()

# Funzione principale per creare grafici per ciascun CSV
def plot_metrics_for_csv(file_path, project_name, box_plot_dir, line_plot_dir):
    if os.path.isfile(file_path):
        df = pd.read_csv(file_path)

        # Creazione delle directory di output se non esistono
        os.makedirs(box_plot_dir, exist_ok=True)
        os.makedirs(line_plot_dir, exist_ok=True)

        # Combinazioni di SENSITIVE e BALANCE
        sensitives = df['COST_SENSITIVE'].unique()
        balances = df['BALANCING'].unique()

        for sensitive in sensitives:
            for balance in balances:
                filtered_df = filter_data(df, sensitive, balance)
                if not filtered_df.empty:
                    title_suffix = f'{project_name} - SENSITIVE_{sensitive} - BALANCE_{balance}'
                    create_box_plot(filtered_df, title_suffix, box_plot_dir)
                    create_line_plot(filtered_df, title_suffix, line_plot_dir)
    else:
        print(f"File not found: {file_path}")

# Percorsi dei file CSV
file_path1 = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/STORM/evaluating/STORM_classifiers_report.csv'
file_path2 = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/BOOKKEEPER/evaluating/BOOKKEEPER_classifiers_report.csv'

# Directory di output per i box plot e i line plot
box_plot_dir1 = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/STORM/charts/box/'
line_plot_dir1 = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/STORM/charts/line/'
box_plot_dir2 = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/BOOKKEEPER/charts/box/'
line_plot_dir2 = '/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/BOOKKEEPER/charts/line/'

# Generazione dei grafici per ciascun CSV
plot_metrics_for_csv(file_path1, "STORM", box_plot_dir1, line_plot_dir1)
plot_metrics_for_csv(file_path2, "BOOKKEEPER", box_plot_dir2, line_plot_dir2)


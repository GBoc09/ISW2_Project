import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import os as os
import shutil as shutil


project_list = ["BOOKKEEPER", "OPENJPA"]
valid_project_list = []

evaluation_path = "/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/{name_1}/evaluating/{name_2}_classifiers_report_.csv"
box_output_image_path = "/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/{name}/charts/box/{imageTitle}.png"
line_output_image_path = "/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/{name}/charts/line/{imageTitle}.png"
comparison_image_path = "/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/{name}/charts/"
image_directory = "/home/giulia/Documenti/GitHub/ISW2_Project/retrieved_data/{name}/charts/"

def main() :
    init_directories()
    analyze_all_projetcs()


def init_directories() :
    for project_name in project_list :
        path = evaluation_path.format(name_1 = project_name, name_2 = project_name)
        if (os.path.exists(path)) :
            valid_project_list.append(project_name)
            directory = image_directory.format(name = project_name)
            if (os.path.isdir(directory)) :
                shutil.rmtree(path = directory)
            os.mkdir(directory)
            os.mkdir(directory + "/" + "box")
            os.mkdir(directory + "/" + "line")
            os.mkdir(directory + "/" + "comparison")



def analyze_all_projetcs() :
    for project_name in valid_project_list :
        analyze_project(project_name)


def analyze_project(project_name):
    dataset_path = evaluation_path.format(name_1=project_name, name_2=project_name)
    dataset = pd.read_csv(dataset_path)

    versions = dataset["#TRAINING_RELEASES"].drop_duplicates().values
    classifiers = dataset["CLASSIFIER"].drop_duplicates().values
    samplers = dataset["BALANCING"].drop_duplicates().values
    sensitive = dataset["COST_SENSITIVE"].drop_duplicates().values

    for sampler in samplers:
        for is_sensitive in sensitive:
            if sampler != "NONE" and is_sensitive:
                continue
            box_plot_data(project_name, dataset, classifiers, sampler, is_sensitive)
            line_plot_data(project_name, dataset, versions, classifiers, sampler, is_sensitive)

    metric_list = ["PRECISION", "RECALL", "AUC"]
    for metric in metric_list:
        comparison_box_plot(project_name, dataset, classifiers, samplers, sensitive, metric)



def comparison_box_plot(project_name, dataset, classifier_list, sampler_list, sensitive_list, metric):
    n_cols = count_columns(sampler_list, sensitive_list)

    figure, axis = plt.subplots(nrows=1, ncols=n_cols, sharey="row", figsize=(20, 10))
    figure.tight_layout(pad=-0.75)

    index = 0
    for sampler, is_sensitive in generate_combinations(sampler_list, sensitive_list):
        if is_sensitive and sampler != "NONE":
            continue

        precision_data_list = get_precision_data_list(dataset, classifier_list, sampler, is_sensitive, metric)

        axis[index].boxplot(precision_data_list)
        axis[index].set_xticklabels(classifier_list, rotation=60)
        axis[index].set_ylim(0, 1)
        axis[index].yaxis.grid(linestyle='--', linewidth=0.75)
        axis[index].set_yticks(np.arange(0, 1.1, 0.05))
        axis[index].set_title(f"Sampling = {sampler}\nSensitive = {is_sensitive}")

        index += 1

    output_path = comparison_image_path.format(name=project_name, imageTitle=project_name + "_" + metric + "_comparison")

    figure.suptitle(project_name + " " + metric + " comparison")
    figure.savefig(output_path)


def count_columns(sampler_list, sensitive_list):
    n_cols = 0
    for sampler in sampler_list:
    	for is_sensitive in sensitive_list:
    	    if not (is_sensitive and sampler != "NONE"):
    	        n_cols += 1
    return n_cols


def generate_combinations(sampler_list, sensitive_list):
        for sampler in sampler_list:
            for is_sensitive in sensitive_list:
                yield sampler, is_sensitive


def get_precision_data_list(dataset, classifier_list, sampler, is_sensitive, metric):
    precision_data_list = []
    for classifier in classifier_list:
        precision_data = get_data(dataset, None, classifier, sampler, is_sensitive)[metric]
        precision_data = precision_data[precision_data.notnull()]
        precision_data_list.append(precision_data)
    return precision_data_list



def line_plot_data(project_name, dataset, version_list, classifier_list, sampler, is_sensitive) :


    #print("Version List:", version_list)
  

    figure, axis = plt.subplots(nrows = 1, ncols = 3)
    figure.set_size_inches(15,5)
    figure.set_tight_layout(tight = {"h_pad" : 0.3})

    title_string = "LinePlot-(Sampler = {samplerName})-(IsSensitive = {sensitive})"
    title_string = title_string.format(samplerName = sampler, sensitive = is_sensitive)
    figure.suptitle(title_string)

    image_path = line_output_image_path.format(name = project_name, imageTitle = title_string)

    for index in range(0, len(classifier_list)) :

        classifier = classifier_list[index]

        data = get_data(dataset, None, classifier,  sampler, is_sensitive)

        recall_data = data["RECALL"]
        precision_data = data["PRECISION"]
        roc_data = data["AUC"]
        # Debug: Print lengths of data arrays
        print(f"Version list length: {len(version_list)}")
        print(f"Recall data length: {len(recall_data)}")
        print(f"Precision data length: {len(precision_data)}")
        print(f"ROC data length: {len(roc_data)}")

        # Check if lengths match
        if len(version_list) == len(recall_data) == len(precision_data) == len(roc_data):
            print(f"Dimensioni corrispondenti per {classifier} con {sampler} e sensitive={is_sensitive}")

            axis[0].plot(version_list, recall_data, label=classifier)
            axis[0].set_title("RECALL")

            axis[1].plot(version_list, precision_data, label=classifier)
            axis[1].set_title("PRECISION")

            axis[2].plot(version_list, roc_data, label=classifier)
            axis[2].set_title("AUC")
        else:
            print(f"Dimensioni non corrispondenti per {classifier} con {sampler} e sensitive={is_sensitive}")

    for i in range(0, 3) :
        axis[i].legend(loc = 'upper left', labels=classifier_list)
        axis[i].grid()
        axis[i].set_yticks(np.arange(0, 1.4, 0.1))
        axis[i].set_xticks(np.arange(0, len(version_list), 1))


    # figure.legend(labels = classifierList)
    figure.savefig(image_path)
    #plt.show()



def box_plot_data(project_name, dataset, classifier_list, sampler, cost_sensitive) :
    figure, axis = plt.subplots(2, 2)
    figure.set_size_inches(9,9)
    #figure.subplots_adjust(hspace=0.3)
    figure.set_tight_layout(tight = {"h_pad" : 0.3})

    title_string = "BoxPlot-((Sampler = {samplerName})-(IsSensitive = {sensitive})"
    title_string = title_string.format(samplerName = sampler, sensitive = cost_sensitive)
    figure.suptitle(title_string)

    image_path = box_output_image_path.format(name = project_name, imageTitle = title_string)

    recall_list = []
    precision_list = []
    roc_list = []
    kappa_list = []
    for classifier in classifier_list :
        data = get_data(dataset, None, classifier, sampler, cost_sensitive)

        precision_data = data["PRECISION"]
        precision_data = precision_data[precision_data.notnull()]

        recall_data = data["RECALL"]
        recall_data = recall_data[recall_data.notnull()]

        roc_data = data["AUC"]
        roc_data = roc_data[roc_data.notnull()]

        kappa_data = data["KAPPA"]
        kappa_data = kappa_data[kappa_data.notnull()]

        recall_list.append(recall_data)
        precision_list.append(precision_data)
        roc_list.append(roc_data)
        kappa_list.append(kappa_data)

    for i in range(0,2) :
        for j in range(0,2) :
            axis[i,j].set_xticklabels(classifier_list, rotation = 15)
            axis[i,j].set_ylim(-0.1, 1)
            axis[i,j].yaxis.grid()
            axis[i,j].set_yticks(np.arange(-0.1,1.1, 0.1))


            if (i == 1 and j == 1) :
                axis[i,j].set_ylim(-0.5, 1)
                axis[i,j].set_yticks(np.arange(-1, 1.1, 0.2))


    axis[0,0].boxplot(recall_list)
    axis[0,0].set_title("RECALL")

    axis[0,1].boxplot(precision_list)
    axis[0,1].set_title("PRECISION")

    axis[1,0].boxplot(roc_list)
    axis[1,0].set_title("AUC")

    axis[1,1].boxplot(kappa_list)
    axis[1,1].set_title("KAPPA")

    figure.savefig(image_path)
    #plt.show()


def get_data(dataset, version, classifier, sampler, is_sensitive) :
    filtered_dataset = dataset
    if (version != None) :
        filtered_dataset = filtered_dataset[(filtered_dataset["#TRAINING_RELEASES"] == version)]
    if (classifier != None) :
        filtered_dataset = filtered_dataset[(filtered_dataset["CLASSIFIER"] == classifier)]
    if (sampler != None) :
        filtered_dataset = filtered_dataset[(filtered_dataset["BALANCING"] == sampler)]
    if (is_sensitive != None) :
        filtered_dataset = filtered_dataset[filtered_dataset["COST_SENSITIVE"] == is_sensitive]

    return filtered_dataset



if __name__ == "__main__" :
    main()

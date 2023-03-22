# Jupyter Notebook Examples

## Getting Started

These notebooks covers the process of creating machine learning models and converting them to ONNX format for later usage inside a Mendix App via [MLKit](https://docs.mendix.com/refguide/machine-learning-kit/).
Please keep in mind that:
* A basic knowledge of python is required
* Depending on the example, some Machine Learning proficiency is required, as per the demonstrated framework in that particular example.
* These were created with AWS Sagemaker, thus some minor changes may be required should you port them to other platforms such as Google Colab, Kaggle or whatnot. Should run out of the box in a local environment.


## List of Models

### Iris Setosa

The standard [dataset](https://www.kaggle.com/datasets/arshid/iris-flower-dataset) for ML introduction is covered in thise files:

* [Decision Trees](iris_dt.ipynb)
* [Logistic Regressors](iris_lr.ipynb)
* [Random Forests](iris_rf.ipynb)
  
  Check the MLKit Demo App for an example for individual integration and ensembling them as well.

### Spam Filter using Multinomial Naive Bayes

[This](spam_nb.ipynb) notebook uses [this](spam.csv) dataset for training a small naive bayes model in order to check spam

### Titanic Survivors

In this [example](titanic_survivors.ipynb), we use the [titanic dataset integrated wiht ONNX pipelines](https://www.kaggle.com/code/wanderfj/titanic-with-sklearn-pipelines) in order to display how a full XGBoost model can be integrated along with preprocessors in a Scikit learn pipeline and exported to ONNX.